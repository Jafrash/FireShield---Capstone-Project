package org.hartford.fireinsurance.infrastructure.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in the system.
 * Domain events represent something that happened in the domain
 * that other bounded contexts or components might be interested in.
 *
 * This follows the Domain-Driven Design (DDD) pattern for
 * achieving loose coupling between domain components.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredAt;
    private final String eventType;
    private final Long aggregateId;
    private final String correlationId;

    /**
     * Constructor for domain events
     * @param aggregateId The ID of the aggregate root that generated this event
     */
    protected DomainEvent(Long aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
        this.aggregateId = aggregateId;
        this.correlationId = UUID.randomUUID().toString();
    }

    /**
     * Constructor with correlation ID for event tracing
     * @param aggregateId The ID of the aggregate root
     * @param correlationId Correlation ID for tracing related events
     */
    protected DomainEvent(Long aggregateId, String correlationId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
        this.aggregateId = aggregateId;
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
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
     * Get event metadata for logging and auditing
     * @return Event metadata as string
     */
    public String getEventMetadata() {
        return String.format("Event{id=%s, type=%s, aggregateId=%d, timestamp=%s, correlationId=%s}",
                           eventId, eventType, aggregateId, occurredAt, correlationId);
    }

    @Override
    public String toString() {
        return getEventMetadata();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainEvent)) return false;
        DomainEvent that = (DomainEvent) o;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return eventId.hashCode();
    }
}