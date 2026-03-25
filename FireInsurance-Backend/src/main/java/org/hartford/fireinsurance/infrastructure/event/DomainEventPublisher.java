package org.hartford.fireinsurance.infrastructure.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Domain Event Publisher implementation that manages the publishing
 * and handling of domain events throughout the application.
 *
 * This component provides:
 * - Synchronous event publishing via Spring's ApplicationEventPublisher
 * - Event storage for debugging and auditing
 * - Correlation ID support for event tracing
 * - Error handling and logging
 *
 * The publisher integrates with Spring's event system while providing
 * domain-specific event management capabilities.
 */
public class DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    // Thread-safe list to store recent events for debugging/auditing
    private final List<DomainEvent> recentEvents = new CopyOnWriteArrayList<>();

    // Maximum number of recent events to keep in memory
    private static final int MAX_RECENT_EVENTS = 1000;

    /**
     * Constructor with Spring's ApplicationEventPublisher
     * @param applicationEventPublisher Spring's event publisher
     */
    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Publish a domain event synchronously
     * @param event The domain event to publish
     */
    public void publish(DomainEvent event) {
        try {
            logger.debug("Publishing domain event: {}", event.getEventMetadata());

            // Store event for debugging/auditing
            storeEvent(event);

            // Publish through Spring's event system
            applicationEventPublisher.publishEvent(event);

            logger.debug("Successfully published domain event: {}", event.getEventId());

        } catch (Exception e) {
            logger.error("Failed to publish domain event: {} - Error: {}",
                        event.getEventMetadata(), e.getMessage(), e);

            // Rethrow as runtime exception to fail fast
            throw new DomainEventPublishingException(
                "Failed to publish domain event: " + event.getEventId(), e);
        }
    }

    /**
     * Publish multiple domain events in sequence
     * @param events List of events to publish
     */
    public void publishAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        logger.debug("Publishing {} domain events", events.size());

        for (DomainEvent event : events) {
            publish(event);
        }

        logger.debug("Successfully published all {} domain events", events.size());
    }

    /**
     * Store event in memory for debugging and auditing
     * @param event The event to store
     */
    private void storeEvent(DomainEvent event) {
        recentEvents.add(event);

        // Remove old events if we exceed the maximum
        while (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.remove(0);
        }
    }

    /**
     * Get recent events for debugging purposes
     * @return List of recent domain events
     */
    public List<DomainEvent> getRecentEvents() {
        return List.copyOf(recentEvents);
    }

    /**
     * Get recent events by type
     * @param eventType The type of events to retrieve
     * @return List of recent events of specified type
     */
    public List<DomainEvent> getRecentEventsByType(String eventType) {
        return recentEvents.stream()
            .filter(event -> eventType.equals(event.getEventType()))
            .toList();
    }

    /**
     * Get recent events for specific aggregate
     * @param aggregateId The aggregate ID to filter by
     * @return List of recent events for the aggregate
     */
    public List<DomainEvent> getRecentEventsForAggregate(Long aggregateId) {
        return recentEvents.stream()
            .filter(event -> aggregateId.equals(event.getAggregateId()))
            .toList();
    }

    /**
     * Get recent events by correlation ID
     * @param correlationId The correlation ID to filter by
     * @return List of related events
     */
    public List<DomainEvent> getRecentEventsByCorrelationId(String correlationId) {
        return recentEvents.stream()
            .filter(event -> correlationId.equals(event.getCorrelationId()))
            .toList();
    }

    /**
     * Clear recent events (useful for testing)
     */
    public void clearRecentEvents() {
        recentEvents.clear();
        logger.debug("Cleared recent events cache");
    }

    /**
     * Get statistics about recent events
     * @return Event statistics
     */
    public EventStatistics getEventStatistics() {
        return new EventStatistics(
            recentEvents.size(),
            recentEvents.stream()
                .map(DomainEvent::getEventType)
                .distinct()
                .count(),
            recentEvents.stream()
                .map(DomainEvent::getAggregateId)
                .distinct()
                .count()
        );
    }

    /**
     * Statistics about domain events
     */
    public static class EventStatistics {
        private final int totalEvents;
        private final long distinctEventTypes;
        private final long distinctAggregates;

        public EventStatistics(int totalEvents, long distinctEventTypes, long distinctAggregates) {
            this.totalEvents = totalEvents;
            this.distinctEventTypes = distinctEventTypes;
            this.distinctAggregates = distinctAggregates;
        }

        public int getTotalEvents() {
            return totalEvents;
        }

        public long getDistinctEventTypes() {
            return distinctEventTypes;
        }

        public long getDistinctAggregates() {
            return distinctAggregates;
        }

        @Override
        public String toString() {
            return String.format("EventStatistics{totalEvents=%d, distinctTypes=%d, distinctAggregates=%d}",
                               totalEvents, distinctEventTypes, distinctAggregates);
        }
    }

    /**
     * Exception thrown when domain event publishing fails
     */
    public static class DomainEventPublishingException extends RuntimeException {
        public DomainEventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}