package org.hartford.fireinsurance.dto;

public class AssignSiuRequest {

    private Long claimId;
    private Long investigatorId;

    public AssignSiuRequest() {
    }

    public AssignSiuRequest(Long claimId, Long investigatorId) {
        this.claimId = claimId;
        this.investigatorId = investigatorId;
    }

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
}