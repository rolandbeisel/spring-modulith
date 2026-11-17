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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.ExternalizationMode;
import org.springframework.modulith.events.OutboxEventScheduler;
import org.springframework.modulith.events.support.OutboxEventRecorder;

/**
 * Configures event recording through an {@link OutboxEventScheduler}. Extensions providing a scheduler
 * through auto-configuration must register its bean definition before this configuration.
 *
 * @author Roland Beisel
 * @since 2.2
 */
@AutoConfiguration(after = EventExternalizationAutoConfiguration.class)
@ConditionalOnProperty(name = ExternalizationMode.PROPERTY, havingValue = "outbox")
@ConditionalOnProperty(name = "spring.modulith.events.externalization.enabled",
		havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(OutboxEventScheduler.class)
public class OutboxEventRecorderAutoConfiguration {

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	@ConditionalOnMissingBean
	OutboxEventRecorder outboxEventRecorder(EventExternalizationConfiguration configuration,
			OutboxEventScheduler scheduler, BeanFactory beanFactory) {
		return new OutboxEventRecorder(configuration, scheduler, beanFactory);
	}
}
