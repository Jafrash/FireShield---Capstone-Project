package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.Claim.FraudStatus;

/**
 * Request DTO for updating investigation notes on a claim.
 */
public class InvestigationNotesRequest {

    private String notes;
    private FraudStatus newStatus;  // Optional - to update fraud status along with notes

    // Constructors
    public InvestigationNotesRequest() {
    }

    public InvestigationNotesRequest(String notes, FraudStatus newStatus) {
        this.notes = notes;
        this.newStatus = newStatus;
    }

    // Getters and Setters
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public FraudStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(FraudStatus newStatus) {
        this.newStatus = newStatus;
    }
}
