package org.hartford.fireinsurance.dto;

/**
 * Request DTO for assigning a claim to SIU (Special Investigations Unit).
 */
public class SiuAssignmentRequest {

    private Long siuInvestigatorId;
    private String initialNotes;

    // Constructors
    public SiuAssignmentRequest() {
    }

    public SiuAssignmentRequest(Long siuInvestigatorId, String initialNotes) {
        this.siuInvestigatorId = siuInvestigatorId;
        this.initialNotes = initialNotes;
    }

    // Getters and Setters
    public Long getSiuInvestigatorId() {
        return siuInvestigatorId;
    }

    public void setSiuInvestigatorId(Long siuInvestigatorId) {
        this.siuInvestigatorId = siuInvestigatorId;
    }

    public String getInitialNotes() {
        return initialNotes;
    }

    public void setInitialNotes(String initialNotes) {
        this.initialNotes = initialNotes;
    }
}
