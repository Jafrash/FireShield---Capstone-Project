package org.hartford.fireinsurance.dto;

public class CreateEndorsementRequest {
    private Long subscriptionId;
    private String changeType;
    private Double requestedCoverage;
    private String newOccupancyType;
    private String newHazardousGoods;
    private String reason;

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Double getRequestedCoverage() {
        return requestedCoverage;
    }

    public void setRequestedCoverage(Double requestedCoverage) {
        this.requestedCoverage = requestedCoverage;
    }

    public String getNewOccupancyType() {
        return newOccupancyType;
    }

    public void setNewOccupancyType(String newOccupancyType) {
        this.newOccupancyType = newOccupancyType;
    }

    public String getNewHazardousGoods() {
        return newHazardousGoods;
    }

    public void setNewHazardousGoods(String newHazardousGoods) {
        this.newHazardousGoods = newHazardousGoods;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
