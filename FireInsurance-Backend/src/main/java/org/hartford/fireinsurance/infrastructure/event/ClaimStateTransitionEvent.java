package org.hartford.fireinsurance.infrastructure.event;

import org.hartford.fireinsurance.domain.state.ClaimState;

/**
 * Domain event that captures claim state transitions.
 * This event is published whenever a claim transitions from one state to another,
 * enabling other components to react to state changes asynchronously.
 *
 * Use cases:
 * - Audit logging of state changes
 * - Notification systems (email, SMS)
 * - Integration with external systems
 * - Analytics and reporting
 * - Workflow automation
 */
public class ClaimStateTransitionEvent extends DomainEvent {

    private final ClaimState fromState;
    private final ClaimState toState;
    private final String reason;
    private final String modifiedBy;
    private final String transitionType;

    /**
     * Constructor for claim state transition event
     * @param claimId The ID of the claim that changed state
     * @param fromState The previous state
     * @param toState The new state
     * @param reason The reason for the transition
     * @param modifiedBy Username of person who initiated the transition
     */
    public ClaimStateTransitionEvent(Long claimId, ClaimState fromState, ClaimState toState,
                                   String reason, String modifiedBy) {
        super(claimId);
        this.fromState = fromState;
        this.toState = toState;
        this.reason = reason;
        this.modifiedBy = modifiedBy;
        this.transitionType = determineTransitionType(fromState, toState);
    }

    /**
     * Constructor with correlation ID for event tracing
     */
    public ClaimStateTransitionEvent(Long claimId, ClaimState fromState, ClaimState toState,
                                   String reason, String modifiedBy, String correlationId) {
        super(claimId, correlationId);
        this.fromState = fromState;
        this.toState = toState;
        this.reason = reason;
        this.modifiedBy = modifiedBy;
        this.transitionType = determineTransitionType(fromState, toState);
    }

    /**
     * Determine the type of transition for categorization
     * @param from Source state
     * @param to Target state
     * @return Transition type category
     */
    private String determineTransitionType(ClaimState from, ClaimState to) {
        if (to.isUnderFraudInvestigation() && !from.isUnderFraudInvestigation()) {
            return "FRAUD_ESCALATION";
        } else if (from.isUnderFraudInvestigation() && !to.isUnderFraudInvestigation()) {
            return "FRAUD_RESOLUTION";
        } else if (to.isTerminal()) {
            return "CLAIM_CLOSURE";
        } else if (to == ClaimState.CONFIRMED_FRAUD) {
            return "FRAUD_CONFIRMATION";
        } else if (to.isSettleable()) {
            return "SETTLEMENT_APPROVAL";
        } else {
            return "STANDARD_PROGRESSION";
        }
    }

    // Getters
    public Long getClaimId() {
        return getAggregateId();
    }

    public ClaimState getFromState() {
        return fromState;
    }

    public ClaimState getToState() {
        return toState;
    }

    public String getReason() {
        return reason;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public String getTransitionType() {
        return transitionType;
    }

    /**
     * Check if this is a fraud-related transition
     * @return true if transition involves fraud investigation
     */
    public boolean isFraudRelated() {
        return "FRAUD_ESCALATION".equals(transitionType) ||
               "FRAUD_RESOLUTION".equals(transitionType) ||
               "FRAUD_CONFIRMATION".equals(transitionType);
    }

    /**
     * Check if this transition indicates claim closure
     * @return true if claim is being closed
     */
    public boolean isClaimClosure() {
        return "CLAIM_CLOSURE".equals(transitionType);
    }

    /**
     * Check if this transition requires notifications
     * @return true if notifications should be sent
     */
    public boolean requiresNotification() {
        return isFraudRelated() || isClaimClosure() || "SETTLEMENT_APPROVAL".equals(transitionType);
    }

    @Override
    public String toString() {
        return String.format("ClaimStateTransition{claimId=%d, %s -> %s, type=%s, by=%s, reason='%s'}",
                           getClaimId(), fromState, toState, transitionType, modifiedBy, reason);
    }
}