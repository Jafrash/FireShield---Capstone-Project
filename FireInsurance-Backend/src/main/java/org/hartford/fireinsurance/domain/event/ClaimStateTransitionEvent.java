package org.hartford.fireinsurance.domain.event;

import org.hartford.fireinsurance.domain.state.ClaimState;

/**
 * Domain event fired when a claim transitions from one state to another.
 * This event enables loose coupling between claim processing and related services.
 */
public class ClaimStateTransitionEvent extends DomainEvent {

    private final ClaimState fromState;
    private final ClaimState toState;
    private final String reason;
    private final String modifiedBy;

    public ClaimStateTransitionEvent(Long claimId, ClaimState fromState, ClaimState toState,
                                    String reason, String modifiedBy) {
        super(claimId);
        this.fromState = fromState;
        this.toState = toState;
        this.reason = reason;
        this.modifiedBy = modifiedBy;
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

    public Long getClaimId() {
        return getAggregateId();
    }

    @Override
    public String getEventDetails() {
        return String.format("Claim %d transitioned from %s to %s. Reason: %s. Modified by: %s",
            getClaimId(), fromState, toState, reason, modifiedBy);
    }

    /**
     * Checks if this transition represents escalation to fraud investigation.
     */
    public boolean isEscalationToFraud() {
        return toState == ClaimState.ESCALATED_TO_SIU
                || toState == ClaimState.SIU_ASSIGNED
                || toState == ClaimState.UNDER_SIU_INVESTIGATION;
    }

    /**
     * Checks if this transition represents fraud investigation completion.
     */
    public boolean isFraudInvestigationComplete() {
        return fromState == ClaimState.UNDER_SIU_INVESTIGATION
                && (toState == ClaimState.CLEARED_BY_SIU || toState == ClaimState.CONFIRMED_FRAUD);
    }

    /**
     * Checks if this transition represents claim settlement.
     */
    public boolean isSettlement() {
        return toState == ClaimState.SETTLED;
    }
}