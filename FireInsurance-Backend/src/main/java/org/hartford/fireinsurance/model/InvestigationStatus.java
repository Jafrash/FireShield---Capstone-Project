package org.hartford.fireinsurance.model;

/**
 * Enum representing the status of an SIU investigation case.
 * Provides clear workflow states for investigation lifecycle management.
 */
public enum InvestigationStatus {
    /**
     * Case has been assigned to an SIU investigator but investigation has not started yet.
     */
    ASSIGNED,

    /**
     * Investigation is actively in progress by the assigned investigator.
     */
    INVESTIGATING,

    /**
     * Investigation work is complete and case is under supervisory review.
     */
    UNDER_REVIEW,

    /**
     * Investigation has been completed and case is closed.
     */
    COMPLETED
}