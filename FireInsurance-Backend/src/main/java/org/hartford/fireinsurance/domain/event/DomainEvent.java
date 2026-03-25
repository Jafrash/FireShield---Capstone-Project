package org.hartford.fireinsurance.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in the fraud detection system.
 * Provides common fields and infrastructure for event-driven architecture.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredAt;
    private final String eventType;
    private final Long aggregateId;
    private final String correlationId;

    protected DomainEvent(Long aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
        this.aggregateId = aggregateId;
        this.correlationId = generateCorrelationId();
    }

    protected DomainEvent(Long aggregateId, String correlationId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
        this.aggregateId = aggregateId;
        this.correlationId = correlationId;
    }

    // Getters
    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Override this method to provide additional event metadata.
     */
    public abstract String getEventDetails();

    /**
     * Generates a correlation ID for tracing related events.
     */
    private String generateCorrelationId() {
        return "CORR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public String toString() {
        return String.format("%s{eventId='%s', aggregateId=%d, occurredAt=%s}",
                eventType, eventId, aggregateId, occurredAt);
    }
}