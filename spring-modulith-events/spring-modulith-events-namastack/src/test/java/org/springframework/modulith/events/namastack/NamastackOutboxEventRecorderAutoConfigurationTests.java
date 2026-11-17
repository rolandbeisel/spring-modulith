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
package org.springframework.modulith.events.namastack;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.namastack.outbox.Outbox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.Externalized;
import org.springframework.modulith.events.OutboxEventScheduler;
import org.springframework.modulith.events.config.EventExternalizationAutoConfiguration;
import org.springframework.modulith.events.config.OutboxEventRecorderAutoConfiguration;
import org.springframework.modulith.events.support.OutboxEventRecorder;

/**
 * Tests the coexistence of the legacy recorder and the recording SPI.
 *
 * @author Roland Beisel
 */
class NamastackOutboxEventRecorderAutoConfigurationTests {

	private final Outbox outbox = mock(Outbox.class);

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(EventExternalizationAutoConfiguration.class,
					OutboxEventRecorderAutoConfiguration.class, NamastackOutboxModeValidationConfiguration.class,
					NamastackOutboxEventRecorderAutoConfiguration.class))
			.withBean(EventExternalizationConfiguration.class, () -> EventExternalizationConfiguration.externalizing()
					.select(EventExternalizationConfiguration.annotatedAsExternalized()).build())
			.withPropertyValues("spring.modulith.events.externalization.mode=outbox");

	@Test
	void retainsLegacyRecordingWithoutScheduler() {

		runner.withBean(Outbox.class, () -> outbox)
				.run(context -> {
					assertThat(context).hasSingleBean(NamastackOutboxEventRecorder.class);
					assertThat(context).doesNotHaveBean(OutboxEventRecorder.class);

					var event = new SampleEvent();
					context.publishEvent(event);

					verify(outbox).schedule(event);
					verifyNoMoreInteractions(outbox);
				});
	}

	@Test
	void recordsOnlyOnceWhenSchedulerReplacesLegacyRecorder() {

		runner.withBean(Outbox.class, () -> outbox)
				.withBean(OutboxEventScheduler.class, () -> (event, key) -> outbox.schedule(event))
				.run(context -> {
					assertThat(context).hasSingleBean(OutboxEventRecorder.class);
					assertThat(context).doesNotHaveBean(NamastackOutboxEventRecorder.class);

					var event = new SampleEvent();
					context.publishEvent(event);

					verify(outbox).schedule(event);
					verifyNoMoreInteractions(outbox);
				});
	}

	@Test
	void doesNotRequireNamastackWhenCustomSchedulerIsPresent() {

		runner.withClassLoader(new FilteredClassLoader("io.namastack"))
				.withBean(OutboxEventScheduler.class, () -> mock(OutboxEventScheduler.class))
				.run(context -> {
					assertThat(context).hasSingleBean(OutboxEventRecorder.class);
					assertThat(context).doesNotHaveBean(NamastackOutboxModeValidationConfiguration.class);
				});
	}

	@Externalized("orders")
	record SampleEvent() {}
}
