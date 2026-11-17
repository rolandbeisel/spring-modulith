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
package org.springframework.modulith.events.jobrunr;

import java.util.List;

import org.jobrunr.scheduling.JobScheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.ExternalizationMode;
import org.springframework.modulith.events.OutboxEventScheduler;
import org.springframework.modulith.events.config.EventExternalizationAutoConfiguration;
import org.springframework.modulith.events.config.OutboxEventRecorderAutoConfiguration;

/**
 * Auto-configuration to set up event externalization via JobRunr.
 *
 * @author Oliver Drotbohm
 * @since 2.1
 * @deprecated since 2.2, for removal. Provide an {@link OutboxEventScheduler} bean through a JobRunr-owned extension.
 */
@Deprecated(since = "2.2", forRemoval = true)
@AutoConfiguration
@AutoConfigureAfter({ EventExternalizationAutoConfiguration.class, OutboxEventRecorderAutoConfiguration.class })
@ConditionalOnProperty(name = ExternalizationMode.PROPERTY, havingValue = "outbox")
@ConditionalOnProperty(name = "spring.modulith.events.externalization.enabled",
		havingValue = "true",
		matchIfMissing = true)
@ConditionalOnBean(JobScheduler.class)
@ConditionalOnMissingBean(OutboxEventScheduler.class)
class JobRunrEventExternalizationAutoConfiguration {

	/**
	 * @deprecated since 2.2, for removal. Provide an {@link OutboxEventScheduler} bean instead.
	 */
	@Deprecated(since = "2.2", forRemoval = true)
	@Bean
	JobRunrEventExternalizer jobRunrEventExternalizer(JobScheduler scheduler,
			List<JobRunrExternalizationTransport> transports, EventExternalizationConfiguration configuration) {
		return new JobRunrEventExternalizer(scheduler, transports, configuration);
	}
}
