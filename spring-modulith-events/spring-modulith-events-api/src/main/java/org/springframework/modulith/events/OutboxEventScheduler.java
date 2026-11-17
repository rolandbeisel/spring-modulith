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
package org.springframework.modulith.events;

import org.jspecify.annotations.Nullable;

/**
 * SPI to schedule selected application events for later externalization. Implementations are registered as Spring beans
 * and invoked synchronously in {@link ExternalizationMode#OUTBOX} mode.
 * <p>
 * Implementations must record the original event before returning and propagate recording failures to the publisher.
 * To provide atomic recording with the business operation, the storage must participate in the publisher's transaction.
 * Behavior outside a transaction is determined by the implementation. Mapping and transport routing are applied when
 * the recorded event is eventually externalized.
 *
 * @author Roland Beisel
 * @since 2.2
 */
@FunctionalInterface
public interface OutboxEventScheduler {

	/**
	 * Schedules the given event for later externalization by recording it synchronously without applying payload mapping.
	 *
	 * @param event the original application event, never {@literal null}.
	 * @param key the routing key resolved against the original event, or {@literal null}. Can be used to group records;
	 *          it does not freeze the routing used for eventual delivery.
	 */
	void schedule(Object event, @Nullable String key);
}
