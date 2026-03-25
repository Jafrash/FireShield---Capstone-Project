package org.hartford.fireinsurance.model;

import jakarta.persistence.*;
import org.hartford.fireinsurance.domain.state.ClaimState;
import org.hartford.fireinsurance.domain.valueobject.FraudAssessment;
import org.hartford.fireinsurance.domain.valueobject.AssignmentTracker;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Enhanced Claim entity with unified state machine architecture and
 * embedded value objects for fraud assessment and assignment tracking.
 *
 * Maintains backward compatibility with existing code while providing
 * new architectural features for improved security and functionality.
 */
@Entity
@Table(name = "claims", indexes = {
    @Index(name = "idx_claim_state", columnList = "current_state"),
    @Index(name = "idx_claim_created_at", columnList = "createdAt"),
    @Index(name = "idx_fraud_score", columnList = "fraud_score")  // Use actual column name
})
public class Claim {

    // Legacy nested ENUMs retained for backward compatibility
    public enum ClaimStatus {
        SUBMITTED,
        UNDER_REVIEW,
        SURVEY_ASSIGNED,
        SURVEY_COMPLETED,
        APPROVED,
        REJECTED,
        PAID,
        INSPECTING,
        INSPECTED,
        SETTLED
    }

    public enum RiskLevel {
        NEGLIGIBLE,  // New level for very low risk
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum FraudStatus {
        CLEAR,
        FLAGGED,
        UNDER_REVIEW,
        SIU_INVESTIGATION,
        CLEARED,
        CONFIRMED_FRAUD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long claimId;

    // NEW: Version field for optimistic locking
    @Version
    private Long version;

    // NEW: Unified state management
    @Enumerated(EnumType.STRING)
    @Column(name = "current_state")
    private ClaimState currentState;

    // NEW: Embedded value objects for enhanced functionality
    @Embedded
    private FraudAssessment fraudAssessment;

    @Embedded
    private AssignmentTracker assignmentTracker;

    // Existing fields
    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private PolicySubscription subscription;

    private LocalDate incidentDate;
    private Double claimAmount;
    private String description;

    @Column(name = "cause_of_fire")
    private String causeOfFire;

    @Column(name = "fir_number")
    private String firNumber;

    @Column(name = "fire_brigade_report_number")
    private String fireBrigadeReportNumber;

    @Column(name = "salvage_details", columnDefinition = "TEXT")
    private String salvageDetails;

    // Legacy fields (maintained for backward compatibility)
    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    // REMOVED: private Double fraudScore; - Now using embedded FraudAssessment only

    @Enumerated(EnumType.STRING)
    @Column(name = "legacy_risk_level")  // Explicit mapping to avoid conflict with embedded object
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    private FraudStatus fraudStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siu_investigator_id")
    private SiuInvestigator siuInvestigator;

    @Column(name = "siu_investigator_id_legacy")
    private Long siuInvestigatorIdLegacy;

    @Column(columnDefinition = "TEXT")
    private String investigationNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;  // NEW: Updated timestamp
    private String createdBy;         // NEW: Audit field
    private String lastModifiedBy;    // NEW: Audit field

    private Double estimatedLoss = 0.0;
    private Double settlementAmount;

    @OneToOne(mappedBy = "claim")
    private ClaimInspection claimInspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "underwriter_id")
    private Underwriter underwriter;

    // Constructors
    public Claim() {
        // Initialize embedded objects
        this.fraudAssessment = new FraudAssessment();
        this.assignmentTracker = new AssignmentTracker();
        this.currentState = ClaimState.SUBMITTED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // NEW: Enhanced constructor with state machine initialization
    public Claim(PolicySubscription subscription, LocalDate incidentDate,
                 Double claimAmount, String description, String createdBy) {
        this();
        this.subscription = subscription;
        this.incidentDate = incidentDate;
        this.claimAmount = claimAmount;
        this.description = description;
        this.createdBy = createdBy;
        this.lastModifiedBy = createdBy;
        this.status = ClaimStatus.SUBMITTED; // Backward compatibility
    }

    // NEW: State machine methods
    public void transitionTo(ClaimState targetState, String reason, String modifiedBy) {
        if (!currentState.canTransitionTo(targetState)) {
            throw new IllegalStateTransitionException(
                String.format("Cannot transition from %s to %s for claim %d. Reason: %s",
                            currentState, targetState, claimId, reason)
            );
        }

        this.currentState = targetState;
        this.lastModifiedBy = modifiedBy;
        this.updatedAt = LocalDateTime.now();

        // Sync legacy status for backward compatibility
        syncLegacyStatus();
    }

    // NEW: Update fraud assessment using value object
    public void updateFraudAssessment(FraudAssessment newAssessment) {
        this.fraudAssessment = newAssessment;
        // Sync legacy fields for backward compatibility
        if (newAssessment != null) {
            // No longer syncing fraudScore as we removed the legacy field
            this.riskLevel = mapToLegacyRiskLevel(newAssessment.getRiskLevel());
        }
        this.updatedAt = LocalDateTime.now();
    }

    // NEW: Business logic methods
    public boolean canBeSettled() {
        return currentState != null && currentState.isSettleable() &&
               settlementAmount != null && settlementAmount > 0;
    }

    public boolean isUnderFraudInvestigation() {
        return currentState != null && currentState.isUnderFraudInvestigation();
    }

    public boolean canAssignUnderwriter() {
        return currentState != null && currentState.allowsUnderwriterAssignment() &&
               (assignmentTracker == null || !assignmentTracker.hasActiveAssignment());
    }

    public boolean canAssignSiu() {
        return currentState != null && currentState.allowsSiuAssignment() &&
               (assignmentTracker == null || !assignmentTracker.hasActiveAssignment());
    }

    // Helper method to sync legacy status with new state
    private void syncLegacyStatus() {
        if (currentState == null) return;

        switch (currentState) {
            case SUBMITTED -> this.status = ClaimStatus.SUBMITTED;
            case UNDER_INITIAL_REVIEW -> this.status = ClaimStatus.UNDER_REVIEW;
            case SURVEY_ASSIGNED -> this.status = ClaimStatus.SURVEY_ASSIGNED;
            case SURVEY_COMPLETED -> this.status = ClaimStatus.SURVEY_COMPLETED;
            case APPROVED_FOR_SETTLEMENT -> this.status = ClaimStatus.APPROVED;
            case REJECTED -> this.status = ClaimStatus.REJECTED;
            case SETTLED -> this.status = ClaimStatus.SETTLED;
            default -> this.status = ClaimStatus.UNDER_REVIEW;
        }
    }

    // Helper method to map new RiskLevel to legacy RiskLevel
    private RiskLevel mapToLegacyRiskLevel(FraudAssessment.RiskLevel newRiskLevel) {
        if (newRiskLevel == null) return RiskLevel.LOW;

        return switch (newRiskLevel) {
            case NEGLIGIBLE -> RiskLevel.NEGLIGIBLE;
            case LOW -> RiskLevel.LOW;
            case MEDIUM -> RiskLevel.MEDIUM;
            case HIGH -> RiskLevel.HIGH;
            case CRITICAL -> RiskLevel.CRITICAL;
        };
    }

    // JPA lifecycle methods
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();

        // Initialize state if not set
        if (currentState == null) {
            currentState = ClaimState.SUBMITTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters (existing + new)
    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    // NEW getters
    public Long getVersion() {
        return version;
    }

    public ClaimState getCurrentState() {
        return currentState;
    }

    public FraudAssessment getFraudAssessment() {
        return fraudAssessment;
    }

    public AssignmentTracker getAssignmentTracker() {
        return assignmentTracker;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    // Existing getters and setters
    public PolicySubscription getSubscription() {
        return subscription;
    }

    public void setSubscription(PolicySubscription subscription) {
        this.subscription = subscription;
    }

    public LocalDate getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(LocalDate incidentDate) {
        this.incidentDate = incidentDate;
    }

    public Double getClaimAmount() {
        return claimAmount;
    }

    public void setClaimAmount(Double claimAmount) {
        this.claimAmount = claimAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCauseOfFire() {
        return causeOfFire;
    }

    public void setCauseOfFire(String causeOfFire) {
        this.causeOfFire = causeOfFire;
    }

    public String getFirNumber() {
        return firNumber;
    }

    public void setFirNumber(String firNumber) {
        this.firNumber = firNumber;
    }

    public String getFireBrigadeReportNumber() {
        return fireBrigadeReportNumber;
    }

    public void setFireBrigadeReportNumber(String fireBrigadeReportNumber) {
        this.fireBrigadeReportNumber = fireBrigadeReportNumber;
    }

    public String getSalvageDetails() {
        return salvageDetails;
    }

    public void setSalvageDetails(String salvageDetails) {
        this.salvageDetails = salvageDetails;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        this.status = status;
    }

    public Double getFraudScore() {
        // Delegate to embedded FraudAssessment object
        return fraudAssessment != null ? fraudAssessment.getFraudScore() : null;
    }

    public void setFraudScore(Double fraudScore) {
        // Update embedded FraudAssessment object only
        if (fraudAssessment != null) {
            fraudAssessment = FraudAssessment.builder()
                .fraudScore(fraudScore)
                .riskLevel(fraudAssessment.getRiskLevel())
                .lastAnalyzed(LocalDateTime.now())
                .analysisDetails(fraudAssessment.getAnalysisDetails())
                .analysisVersion(fraudAssessment.getAnalysisVersion())
                .build();
        } else {
            // Initialize new FraudAssessment if null
            fraudAssessment = FraudAssessment.builder()
                .fraudScore(fraudScore)
                .riskLevel(FraudAssessment.RiskLevel.LOW)
                .lastAnalyzed(LocalDateTime.now())
                .build();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ClaimInspection getClaimInspection() {
        return claimInspection;
    }

    public void setClaimInspection(ClaimInspection claimInspection) {
        this.claimInspection = claimInspection;
    }

    public Double getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(Double settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public Double getEstimatedLoss() {
        return estimatedLoss;
    }

    public void setEstimatedLoss(Double estimatedLoss) {
        this.estimatedLoss = estimatedLoss;
    }

    public Underwriter getUnderwriter() {
        return underwriter;
    }

    public void setUnderwriter(Underwriter underwriter) {
        this.underwriter = underwriter;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public FraudStatus getFraudStatus() {
        return fraudStatus;
    }

    public void setFraudStatus(FraudStatus fraudStatus) {
        this.fraudStatus = fraudStatus;
    }

    public SiuInvestigator getSiuInvestigator() {
        return siuInvestigator;
    }

    public void setSiuInvestigator(SiuInvestigator siuInvestigator) {
        this.siuInvestigator = siuInvestigator;
    }

    public Long getSiuInvestigatorIdLegacy() {
        return siuInvestigatorIdLegacy;
    }

    public void setSiuInvestigatorIdLegacy(Long siuInvestigatorIdLegacy) {
        this.siuInvestigatorIdLegacy = siuInvestigatorIdLegacy;
    }

    public Long getSiuInvestigatorId() {
        return siuInvestigator != null ? siuInvestigator.getInvestigatorId() : siuInvestigatorIdLegacy;
    }

    public String getInvestigationNotes() {
        return investigationNotes;
    }

    public void setInvestigationNotes(String investigationNotes) {
        this.investigationNotes = investigationNotes;
    }

    public LocalDateTime getFraudAnalysisTimestamp() {
        // Delegate to embedded FraudAssessment for backward compatibility
        return fraudAssessment != null ? fraudAssessment.getLastAnalyzed() : null;
    }

    public void setFraudAnalysisTimestamp(LocalDateTime fraudAnalysisTimestamp) {
        // Delegate to embedded FraudAssessment for backward compatibility
        if (fraudAssessment != null && fraudAnalysisTimestamp != null) {
            // Rebuild the assessment with the new timestamp
            this.fraudAssessment = FraudAssessment.builder()
                .fraudScore(fraudAssessment.getFraudScore())
                .riskLevel(fraudAssessment.getRiskLevel())
                .analysisDetails(fraudAssessment.getAnalysisDetails())
                .analysisVersion(fraudAssessment.getAnalysisVersion())
                .lastAnalyzed(fraudAnalysisTimestamp)
                .build();
        }
    }

    // Exception class
    public static class IllegalStateTransitionException extends RuntimeException {
        public IllegalStateTransitionException(String message) {
            super(message);
        }
    }
}