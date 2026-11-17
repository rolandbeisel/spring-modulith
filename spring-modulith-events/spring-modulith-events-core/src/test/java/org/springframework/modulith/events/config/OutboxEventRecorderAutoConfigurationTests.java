/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.modulith.events.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.Externalized;
import org.springframework.modulith.events.OutboxEventScheduler;
import org.springframework.modulith.events.support.OutboxEventRecorder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests for the opt-in recording SPI and its synchronous invocation contract.
 *
 * @author Roland Beisel
 */
class OutboxEventRecorderAutoConfigurationTests {

	private final OutboxEventScheduler scheduler = mock(OutboxEventScheduler.class);

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(EventExternalizationAutoConfiguration.class,
					OutboxEventRecorderAutoConfiguration.class))
			.withBean(EventExternalizationConfiguration.class, () -> EventExternalizationConfiguration.externalizing()
					.select(EventExternalizationConfiguration.annotatedAsExternalized())
					.build());

	@Test
	void doesNotRegisterRecorderWithoutScheduler() {

		runner.withPropertyValues("spring.modulith.events.externalization.mode=outbox")
				.run(context -> assertThat(context).doesNotHaveBean(OutboxEventRecorder.class));
	}

	@Test
	void doesNotRecordInModuleListenerMode() {

		runner.withBean(OutboxEventScheduler.class, () -> scheduler)
				.run(context -> {
					assertThat(context).doesNotHaveBean(OutboxEventRecorder.class);
					context.publishEvent(new UnkeyedEvent());
					verifyNoInteractions(scheduler);
				});
	}

	@Test
	void honorsDisabledExternalization() {

		runner.withBean(OutboxEventScheduler.class, () -> scheduler)
				.withPropertyValues("spring.modulith.events.externalization.mode=outbox",
						"spring.modulith.events.externalization.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(OutboxEventRecorder.class));
	}

	@Test
	void recordsOriginalEventAndResolvesKeyWithoutMapping() {

		var configuration = EventExternalizationConfiguration.externalizing()
				.select(EventExternalizationConfiguration.annotatedAsExternalized())
				.mapping(KeyedEvent.class, event -> {
					throw new AssertionError("Mapping must be deferred until delivery!");
				})
				.build();

		runner.withAllowBeanDefinitionOverriding(true)
				.withBean(EventExternalizationConfiguration.class, () -> configuration)
				.withBean("keyPrefix", String.class, () -> "order-")
				.withBean(OutboxEventScheduler.class, () -> scheduler)
				.withPropertyValues("spring.modulith.events.externalization.mode=outbox")
				.run(context -> {

					var keyed = new KeyedEvent("42");
					var unkeyed = new UnkeyedEvent();

					context.publishEvent(keyed);
					context.publishEvent(unkeyed);
					context.publishEvent("not selected");

					verify(scheduler).schedule(same(keyed), eq("order-42"));
					verify(scheduler).schedule(same(unkeyed), isNull());
					verifyNoMoreInteractions(scheduler);
				});
	}

	@Test
	void discoversSchedulerProvidedByExtensionAutoConfiguration() {

		runner.withConfiguration(AutoConfigurations.of(SampleExtension.class))
				.withPropertyValues("spring.modulith.events.externalization.mode=outbox")
				.run(context -> {
					assertThat(context).hasSingleBean(OutboxEventRecorder.class);
					var event = new UnkeyedEvent();
					context.publishEvent(event);
					verify(context.getBean(OutboxEventScheduler.class)).schedule(event, null);
				});
	}

	@Test
	void rejectsAmbiguousSchedulers() {

		runner.withBean("first", OutboxEventScheduler.class, () -> scheduler)
				.withBean("second", OutboxEventScheduler.class, () -> mock(OutboxEventScheduler.class))
				.withPropertyValues("spring.modulith.events.externalization.mode=outbox")
				.run(context -> assertThat(context).hasFailed()
						.getFailure().hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class));
	}

	@Test
	void recordsOnPublisherThreadAndPropagatesFailureToTransaction() {

		var failure = new IllegalStateException("Recording failed!");
		var publisherThread = Thread.currentThread();
		OutboxEventScheduler failingScheduler = (event, key) -> {
			assertThat(Thread.currentThread()).isSameAs(publisherThread);
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
			throw failure;
		};

		runner.withBean(OutboxEventScheduler.class, () -> failingScheduler)
				.withBean("applicationEventMulticaster", SimpleApplicationEventMulticaster.class, () -> {
					var multicaster = new SimpleApplicationEventMulticaster();
					multicaster.setTaskExecutor(task -> {
						throw new AssertionError("Recording must not use the event executor!");
					});
					return multicaster;
				})
				.withPropertyValues("spring.modulith.events.externalization.mode=outbox")
				.run(context -> {

					var manager = new RecordingTransactionManager();
					var transaction = new TransactionTemplate(manager);

					assertThatThrownBy(() -> transaction.executeWithoutResult(
							status -> context.publishEvent(new UnkeyedEvent())))
							.isSameAs(failure);

					assertThat(manager.rolledBack).isTrue();
				});
	}

	@AutoConfiguration(before = OutboxEventRecorderAutoConfiguration.class)
	static class SampleExtension {

		@Bean
		OutboxEventScheduler scheduler() {
			return mock(OutboxEventScheduler.class);
		}
	}

	@Externalized("orders::#{@keyPrefix + id()}")
	record KeyedEvent(String id) {}

	@Externalized("orders")
	record UnkeyedEvent() {}

	private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {

		private boolean rolledBack;

		@Override
		protected Object doGetTransaction() {
			return new Object();
		}

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) {}

		@Override
		protected void doCommit(DefaultTransactionStatus status) {
			throw new AssertionError("Failed recording must roll back!");
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) {
			rolledBack = true;
		}
	}
}
