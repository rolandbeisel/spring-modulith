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
package org.springframework.modulith.events.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.OutboxEventScheduler;
import org.springframework.util.Assert;

/**
 * Selects application events and synchronously delegates their recording to an
 * {@link OutboxEventScheduler}. Payload mapping is deferred until externalization.
 *
 * @author Roland Beisel
 * @since 2.2
 */
public class OutboxEventRecorder implements ApplicationListener<PayloadApplicationEvent<?>> {

	private final EventExternalizationConfiguration configuration;
	private final OutboxEventScheduler scheduler;
	private final EvaluationContext context;

	/**
	 * Creates a new {@link OutboxEventRecorder}.
	 *
	 * @param configuration must not be {@literal null}.
	 * @param scheduler must not be {@literal null}.
	 * @param beanFactory must not be {@literal null}.
	 */
	public OutboxEventRecorder(EventExternalizationConfiguration configuration,
			OutboxEventScheduler scheduler, BeanFactory beanFactory) {

		Assert.notNull(configuration, "EventExternalizationConfiguration must not be null!");
		Assert.notNull(scheduler, "OutboxEventScheduler must not be null!");
		Assert.notNull(beanFactory, "BeanFactory must not be null!");

		var context = new StandardEvaluationContext();
		context.setBeanResolver(new BeanFactoryResolver(beanFactory));

		this.configuration = configuration;
		this.scheduler = scheduler;
		this.context = context;
	}

	@Override
	public void onApplicationEvent(PayloadApplicationEvent<?> event) {

		var payload = event.getPayload();

		if (!configuration.supports(payload)) {
			return;
		}

		var target = configuration.determineTarget(payload);
		var routing = BrokerRouting.of(target, context);

		scheduler.schedule(payload, routing.getKey(payload));
	}

	@Override
	public boolean supportsAsyncExecution() {
		return false;
	}
}
