package org.hartford.fireinsurance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an SIU investigation case.
 * Tracks the complete lifecycle of a fraud investigation from assignment to completion.
 */
@Entity
@Table(name = "investigation_cases")
public class InvestigationCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long investigationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_investigator_id")
    private SiuInvestigator assignedInvestigator;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvestigationStatus status = InvestigationStatus.ASSIGNED;

    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;

    @Column(name = "started_date")
    private LocalDateTime startedDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "initial_notes", columnDefinition = "TEXT")
    private String initialNotes;

    @Column(name = "priority_level")
    private Integer priorityLevel = 3; // 1=Critical, 2=High, 3=Medium, 4=Low, 5=Lowest

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    // Constructors
    public InvestigationCase() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public InvestigationCase(Claim claim, SiuInvestigator assignedInvestigator, String initialNotes) {
        this();
        this.claim = claim;
        this.assignedInvestigator = assignedInvestigator;
        this.initialNotes = initialNotes;
        this.assignedDate = LocalDateTime.now();
        this.status = InvestigationStatus.ASSIGNED;
    }

    // Pre-persist and pre-update hooks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (assignedDate == null && assignedInvestigator != null) {
            assignedDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to start investigation
    public void startInvestigation() {
        if (this.status == InvestigationStatus.ASSIGNED) {
            this.status = InvestigationStatus.INVESTIGATING;
            this.startedDate = LocalDateTime.now();
        }
    }

    // Helper method to complete investigation
    public void completeInvestigation() {
        this.status = InvestigationStatus.COMPLETED;
        this.completedDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getInvestigationId() {
        return investigationId;
    }

    public void setInvestigationId(Long investigationId) {
        this.investigationId = investigationId;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
    }

    public SiuInvestigator getAssignedInvestigator() {
        return assignedInvestigator;
    }

    public void setAssignedInvestigator(SiuInvestigator assignedInvestigator) {
        this.assignedInvestigator = assignedInvestigator;
    }

    public InvestigationStatus getStatus() {
        return status;
    }

    public void setStatus(InvestigationStatus status) {
        this.status = status;
    }

    public LocalDateTime getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDateTime assignedDate) {
        this.assignedDate = assignedDate;
    }

    public LocalDateTime getStartedDate() {
        return startedDate;
    }

    public void setStartedDate(LocalDateTime startedDate) {
        this.startedDate = startedDate;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public String getInitialNotes() {
        return initialNotes;
    }

    public void setInitialNotes(String initialNotes) {
        this.initialNotes = initialNotes;
    }

    public Integer getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(Integer priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "InvestigationCase{" +
                "investigationId=" + investigationId +
                ", claimId=" + (claim != null ? claim.getClaimId() : null) +
                ", assignedInvestigator=" + (assignedInvestigator != null ? assignedInvestigator.getInvestigatorId() : null) +
                ", status=" + status +
                ", priorityLevel=" + priorityLevel +
                ", assignedDate=" + assignedDate +
                '}';
    }
}