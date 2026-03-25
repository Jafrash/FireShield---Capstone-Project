package org.hartford.fireinsurance.domain.state;

import java.util.EnumSet;
import java.util.Set;

/**
 * Unified state machine for claims in the fraud detection system.
 * This replaces the separate ClaimStatus and FraudStatus enums with a single source of truth.
 *
 * Each state defines its allowed transitions, enforcing proper workflow progression
 * and preventing invalid state changes.
 */
public enum ClaimState {

    // ========== Initial States ==========
    SUBMITTED("Initial claim submission"),

    // ========== Standard Processing States ==========
    UNDER_INITIAL_REVIEW("Initial underwriter review"),
    APPROVED_FOR_SURVEY("Survey assignment pending"),
    SURVEY_ASSIGNED("Survey in progress"),
    SURVEY_COMPLETED("Survey done, decision pending"),

    // ========== Fraud Investigation States ==========
    FLAGGED_FOR_REVIEW("Flagged for enhanced review"),
    ESCALATED_TO_SIU("SIU investigation required"),
    SIU_ASSIGNED("SIU investigator assigned"),
    UNDER_SIU_INVESTIGATION("Active SIU investigation"),
    SIU_INVESTIGATION_COMPLETED("SIU investigation done"),
    CLEARED_BY_REVIEW("Cleared after enhanced review"),
    CLEARED_BY_SIU("Cleared after SIU investigation"),

    // ========== Terminal States ==========
    APPROVED_FOR_SETTLEMENT("Ready for settlement payment"),
    REJECTED("Claim rejected"),
    SETTLED("Claim settled and paid"),
    CONFIRMED_FRAUD("Confirmed fraud - blacklisted"),
    BLACKLISTED("Entity blacklisted");

    private final String description;
    private Set<ClaimState> allowedTransitions;

    ClaimState(String description) {
        this.description = description;
    }

    // Static initialization block to set up transitions after all enum constants are created
    static {
        SUBMITTED.allowedTransitions = EnumSet.of(UNDER_INITIAL_REVIEW, REJECTED);

        UNDER_INITIAL_REVIEW.allowedTransitions = EnumSet.of(
            APPROVED_FOR_SURVEY, FLAGGED_FOR_REVIEW, ESCALATED_TO_SIU, REJECTED);

        APPROVED_FOR_SURVEY.allowedTransitions = EnumSet.of(
            SURVEY_ASSIGNED, FLAGGED_FOR_REVIEW);

        SURVEY_ASSIGNED.allowedTransitions = EnumSet.of(
            SURVEY_COMPLETED, FLAGGED_FOR_REVIEW);

        SURVEY_COMPLETED.allowedTransitions = EnumSet.of(
            APPROVED_FOR_SETTLEMENT, FLAGGED_FOR_REVIEW, ESCALATED_TO_SIU, REJECTED);

        FLAGGED_FOR_REVIEW.allowedTransitions = EnumSet.of(
            ESCALATED_TO_SIU, CLEARED_BY_REVIEW, REJECTED);

        ESCALATED_TO_SIU.allowedTransitions = EnumSet.of(SIU_ASSIGNED);

        SIU_ASSIGNED.allowedTransitions = EnumSet.of(UNDER_SIU_INVESTIGATION);

        UNDER_SIU_INVESTIGATION.allowedTransitions = EnumSet.of(SIU_INVESTIGATION_COMPLETED);

        SIU_INVESTIGATION_COMPLETED.allowedTransitions = EnumSet.of(
            CLEARED_BY_SIU, CONFIRMED_FRAUD, REJECTED);

        CLEARED_BY_REVIEW.allowedTransitions = EnumSet.of(APPROVED_FOR_SETTLEMENT);

        CLEARED_BY_SIU.allowedTransitions = EnumSet.of(APPROVED_FOR_SETTLEMENT);

        APPROVED_FOR_SETTLEMENT.allowedTransitions = EnumSet.of(SETTLED);

        // Terminal states have no transitions
        REJECTED.allowedTransitions = EnumSet.noneOf(ClaimState.class);
        SETTLED.allowedTransitions = EnumSet.noneOf(ClaimState.class);
        CONFIRMED_FRAUD.allowedTransitions = EnumSet.of(BLACKLISTED);
        BLACKLISTED.allowedTransitions = EnumSet.noneOf(ClaimState.class);
    }

    /**
     * Check if transition to another state is allowed.
     * @param targetState The state to transition to
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(ClaimState targetState) {
        return allowedTransitions.contains(targetState);
    }

    /**
     * Get all states this state can transition to.
     * @return Set of allowed target states
     */
    public Set<ClaimState> getAllowedTransitions() {
        return EnumSet.copyOf(allowedTransitions);
    }

    /**
     * Check if this state represents a terminal state (no further transitions allowed).
     * @return true if this is a terminal state
     */
    public boolean isTerminal() {
        return allowedTransitions.isEmpty();
    }

    /**
     * Check if this state allows settlement processing.
     * @return true if claim can be settled from this state
     */
    public boolean isSettleable() {
        return this == APPROVED_FOR_SETTLEMENT;
    }

    /**
     * Check if this state represents an active fraud investigation.
     * @return true if claim is under fraud investigation
     */
    public boolean isUnderFraudInvestigation() {
        return this == UNDER_SIU_INVESTIGATION || this == SIU_ASSIGNED || this == ESCALATED_TO_SIU;
    }

    /**
     * Check if this state allows underwriter assignment.
     * @return true if underwriter can be assigned
     */
    public boolean allowsUnderwriterAssignment() {
        return this == SUBMITTED || this == UNDER_INITIAL_REVIEW || this == FLAGGED_FOR_REVIEW;
    }

    /**
     * Check if this state allows SIU assignment.
     * @return true if SIU investigator can be assigned
     */
    public boolean allowsSiuAssignment() {
        return this == ESCALATED_TO_SIU;
    }

    /**
     * Check if this state requires elevated permissions to modify.
     * @return true if only admins/supervisors can change this state
     */
    public boolean requiresElevatedPermissions() {
        return this == CONFIRMED_FRAUD || this == BLACKLISTED || this == UNDER_SIU_INVESTIGATION;
    }

    /**
     * Check if this state blocks normal claim processing.
     * @return true if normal processing is blocked
     */
    public boolean blocksNormalProcessing() {
        return this == UNDER_SIU_INVESTIGATION || this == ESCALATED_TO_SIU ||
               this == SIU_ASSIGNED || this == CONFIRMED_FRAUD || this == BLACKLISTED;
    }

    /**
     * Check if this state is fraud-related.
     * @return true if this state is part of fraud investigation workflow
     */
    public boolean isFraudRelated() {
        return this == FLAGGED_FOR_REVIEW || this == ESCALATED_TO_SIU ||
               this == SIU_ASSIGNED || this == UNDER_SIU_INVESTIGATION ||
               this == SIU_INVESTIGATION_COMPLETED || this == CONFIRMED_FRAUD ||
               this == BLACKLISTED || this == CLEARED_BY_SIU;
    }

    /**
     * Get the user-friendly description of this state.
     * @return State description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the category this state belongs to.
     * @return State category for UI grouping
     */
    public StateCategory getCategory() {
        return switch (this) {
            case SUBMITTED -> StateCategory.INITIAL;
            case UNDER_INITIAL_REVIEW, APPROVED_FOR_SURVEY, SURVEY_ASSIGNED, SURVEY_COMPLETED ->
                StateCategory.PROCESSING;
            case FLAGGED_FOR_REVIEW, ESCALATED_TO_SIU, SIU_ASSIGNED, UNDER_SIU_INVESTIGATION,
                 SIU_INVESTIGATION_COMPLETED, CLEARED_BY_REVIEW, CLEARED_BY_SIU ->
                StateCategory.INVESTIGATION;
            case APPROVED_FOR_SETTLEMENT -> StateCategory.APPROVAL;
            case SETTLED -> StateCategory.COMPLETED;
            case REJECTED, CONFIRMED_FRAUD, BLACKLISTED -> StateCategory.TERMINAL;
        };
    }

    /**
     * Enum for categorizing states for UI purposes.
     */
    public enum StateCategory {
        INITIAL("Initial"),
        PROCESSING("Processing"),
        INVESTIGATION("Investigation"),
        APPROVAL("Approval"),
        COMPLETED("Completed"),
        TERMINAL("Terminal");

        private final String displayName;

        StateCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name(), description);
    }
}