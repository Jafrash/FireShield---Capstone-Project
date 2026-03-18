package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.PolicyEndorsement;

import java.time.LocalDateTime;

public class EndorsementResponse {
    private Long endorsementId;
    private Long subscriptionId;
    private String changeType;
    private Double requestedCoverage;
    private String newOccupancyType;
    private String newHazardousGoods;
    private String reason;
    private PolicyEndorsement.EndorsementStatus status;
    private String requestedBy;
    private String reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public Long getEndorsementId() {
        return endorsementId;
    }

    public void setEndorsementId(Long endorsementId) {
        this.endorsementId = endorsementId;
    }

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

    public PolicyEndorsement.EndorsementStatus getStatus() {
        return status;
    }

    public void setStatus(PolicyEndorsement.EndorsementStatus status) {
        this.status = status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
