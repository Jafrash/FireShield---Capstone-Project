package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.InvestigationActivity;

/**
 * Request DTO for adding timeline entries to investigation cases.
 */
public class AddTimelineEntryRequest {

    private InvestigationActivity.ActivityType activityType;
    private String description;
    private String additionalDetails;

    // Constructors
    public AddTimelineEntryRequest() {
    }

    public AddTimelineEntryRequest(InvestigationActivity.ActivityType activityType, String description) {
        this.activityType = activityType;
        this.description = description;
    }

    public AddTimelineEntryRequest(InvestigationActivity.ActivityType activityType, String description, String additionalDetails) {
        this.activityType = activityType;
        this.description = description;
        this.additionalDetails = additionalDetails;
    }

    // Getters and Setters
    public InvestigationActivity.ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(InvestigationActivity.ActivityType activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdditionalDetails() {
        return additionalDetails;
    }

    public void setAdditionalDetails(String additionalDetails) {
        this.additionalDetails = additionalDetails;
    }
}