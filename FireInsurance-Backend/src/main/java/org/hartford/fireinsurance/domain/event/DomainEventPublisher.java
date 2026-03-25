package org.hartford.fireinsurance.domain.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain Event Publisher for the fraud detection system.
 * Provides a clean interface for publishing domain events with proper transaction handling.
 */
@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public DomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publishes a domain event. Events are published after the current transaction commits.
     * This ensures event handlers only process events from successfully committed transactions.
     *
     * @param event the domain event to publish
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(DomainEvent event) {
        try {
            log.info("Publishing domain event: {} for aggregate: {}",
                event.getEventType(), event.getAggregateId());

            // Publish the event - Spring will handle async processing
            eventPublisher.publishEvent(event);

            log.debug("Successfully published event: {} with ID: {}",
                event.getEventType(), event.getEventId());

        } catch (Exception e) {
            log.error("Failed to publish domain event: {} for aggregate: {}. Error: {}",
                event.getEventType(), event.getAggregateId(), e.getMessage(), e);

            // Re-throw to ensure transaction rollback if event publishing fails
            throw new DomainEventPublishingException("Failed to publish domain event", e);
        }
    }

    /**
     * Publishes multiple domain events in a single transaction.
     *
     * @param events the domain events to publish
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishAll(DomainEvent... events) {
        for (DomainEvent event : events) {
            publish(event);
        }
    }

    /**
     * Exception thrown when domain event publishing fails.
     */
    public static class DomainEventPublishingException extends RuntimeException {
        public DomainEventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}