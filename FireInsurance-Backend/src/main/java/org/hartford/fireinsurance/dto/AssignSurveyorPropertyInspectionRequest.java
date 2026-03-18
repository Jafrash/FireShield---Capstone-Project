package org.hartford.fireinsurance.dto;

import jakarta.validation.constraints.NotNull;

public class AssignSurveyorPropertyInspectionRequest {

    @NotNull
    private Long subscriptionId;

    @NotNull
    private Long surveyorId;

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getSurveyorId() {
        return surveyorId;
    }

    public void setSurveyorId(Long surveyorId) {
        this.surveyorId = surveyorId;
    }
}

