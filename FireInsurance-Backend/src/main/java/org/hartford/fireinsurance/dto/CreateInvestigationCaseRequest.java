package org.hartford.fireinsurance.dto;

/**
 * Request DTO for creating a new investigation case.
 */
public class CreateInvestigationCaseRequest {

    private Long claimId;
    private Long investigatorId;
    private String initialNotes;

    // Constructors
    public CreateInvestigationCaseRequest() {
    }

    public CreateInvestigationCaseRequest(Long claimId, Long investigatorId, String initialNotes) {
        this.claimId = claimId;
        this.investigatorId = investigatorId;
        this.initialNotes = initialNotes;
    }

    // Getters and Setters
    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public Long getInvestigatorId() {
        return investigatorId;
    }

    public void setInvestigatorId(Long investigatorId) {
        this.investigatorId = investigatorId;
    }

    public String getInitialNotes() {
        return initialNotes;
    }

    public void setInitialNotes(String initialNotes) {
        this.initialNotes = initialNotes;
    }
}