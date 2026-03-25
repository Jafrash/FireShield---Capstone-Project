package org.hartford.fireinsurance.dto;

/**
 * DTO for SIU investigation action requests.
 * Used for start investigation, mark as fraud, and clear claim actions.
 */
public class SiuInvestigationActionRequest {

    private String claimId;
    private String investigatorId;
    private String notes;
    private String reason;

    // Constructors
    public SiuInvestigationActionRequest() {}

    public SiuInvestigationActionRequest(String claimId, String investigatorId, String notes, String reason) {
        this.claimId = claimId;
        this.investigatorId = investigatorId;
        this.notes = notes;
        this.reason = reason;
    }

    // Getters and Setters
    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getInvestigatorId() {
        return investigatorId;
    }

    public void setInvestigatorId(String investigatorId) {
        this.investigatorId = investigatorId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "SiuInvestigationActionRequest{" +
                "claimId='" + claimId + '\'' +
                ", investigatorId='" + investigatorId + '\'' +
                ", notes='" + notes + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}