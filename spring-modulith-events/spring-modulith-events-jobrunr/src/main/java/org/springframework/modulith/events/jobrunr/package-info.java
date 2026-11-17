/**
 * Legacy JobRunr event externalization support.
 * <p>
 * Deprecated since 2.2, for removal. Integrations should be provided by JobRunr-owned extensions implementing
 * {@link org.springframework.modulith.events.OutboxEventScheduler} and invoking
 * {@link org.springframework.modulith.events.support.OutboxEventExternalizer} for delivery.
 */
@org.jspecify.annotations.NullMarked
package org.springframework.modulith.events.jobrunr;
