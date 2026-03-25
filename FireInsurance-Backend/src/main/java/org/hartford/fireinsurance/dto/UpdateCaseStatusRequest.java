package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.InvestigationStatus;

/**
 * Request DTO for updating investigation case status.
 */
public class UpdateCaseStatusRequest {

    private InvestigationStatus status;

    // Constructors
    public UpdateCaseStatusRequest() {
    }

    public UpdateCaseStatusRequest(InvestigationStatus status) {
        this.status = status;
    }

    // Getters and Setters
    public InvestigationStatus getStatus() {
        return status;
    }

    public void setStatus(InvestigationStatus status) {
        this.status = status;
    }
}