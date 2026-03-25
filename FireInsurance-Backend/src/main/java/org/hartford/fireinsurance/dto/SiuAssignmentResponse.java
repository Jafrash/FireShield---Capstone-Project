package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.Claim;

/**
 * Simple response DTO for SIU assignment to avoid JSON circular references
 */
public class SiuAssignmentResponse {
    private Long claimId;
    private String status;
    private String fraudStatus;
    private Long siuInvestigatorId;
    private String message;

    public SiuAssignmentResponse() {
    }

    public SiuAssignmentResponse(Claim claim) {
        this.claimId = claim.getClaimId();
        this.status = claim.getStatus() != null ? claim.getStatus().toString() : "UNKNOWN";
        this.fraudStatus = claim.getFraudStatus() != null ? claim.getFraudStatus().toString() : "UNKNOWN";
        this.siuInvestigatorId = claim.getSiuInvestigatorId();
        this.message = "Claim successfully assigned to SIU investigator";
    }

    public SiuAssignmentResponse(Long claimId, String message) {
        this.claimId = claimId;
        this.message = message;
    }

    // Getters and setters
    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFraudStatus() {
        return fraudStatus;
    }

    public void setFraudStatus(String fraudStatus) {
        this.fraudStatus = fraudStatus;
    }

    public Long getSiuInvestigatorId() {
        return siuInvestigatorId;
    }

    public void setSiuInvestigatorId(Long siuInvestigatorId) {
        this.siuInvestigatorId = siuInvestigatorId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}