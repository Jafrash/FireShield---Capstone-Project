package org.hartford.fireinsurance.dto;

import jakarta.validation.constraints.NotNull;

public class AssignSurveyorClaimInspectionRequest {

    @NotNull
    private Long claimId;

    @NotNull
    private Long surveyorId;

    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public Long getSurveyorId() {
        return surveyorId;
    }

    public void setSurveyorId(Long surveyorId) {
        this.surveyorId = surveyorId;
    }
}

