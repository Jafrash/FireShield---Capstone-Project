package org.hartford.fireinsurance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "policy_endorsements")
public class PolicyEndorsement {

    public enum EndorsementStatus {
        REQUESTED,
        APPROVED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "endorsement_id")
    private Long endorsementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private PolicySubscription subscription;

    @Column(name = "change_type")
    private String changeType;

    @Column(name = "requested_coverage")
    private Double requestedCoverage;

    @Column(name = "new_occupancy_type")
    private String newOccupancyType;

    @Column(name = "new_hazardous_goods", columnDefinition = "TEXT")
    private String newHazardousGoods;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EndorsementStatus status;

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public Long getEndorsementId() {
        return endorsementId;
    }

    public void setEndorsementId(Long endorsementId) {
        this.endorsementId = endorsementId;
    }

    public PolicySubscription getSubscription() {
        return subscription;
    }

    public void setSubscription(PolicySubscription subscription) {
        this.subscription = subscription;
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

    public EndorsementStatus getStatus() {
        return status;
    }

    public void setStatus(EndorsementStatus status) {
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
