package org.hartford.fireinsurance.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
public class Claim {

    // Nested ENUM for Claim Status
    public enum ClaimStatus {
        SUBMITTED,
        UNDER_REVIEW,
        SURVEY_ASSIGNED,
        SURVEY_COMPLETED,
        APPROVED,
        REJECTED,
        PAID,
        // Legacy statuses retained for backward compatibility
        INSPECTING,
        INSPECTED,
        SETTLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long claimId;

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

    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    private Double fraudScore;

    private LocalDateTime createdAt;

    private Double estimatedLoss = 0.0;

    private Double settlementAmount;

    @OneToOne(mappedBy = "claim")
    private ClaimInspection claimInspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "underwriter_id")
    private Underwriter underwriter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siu_investigator_id")
    private SiuInvestigator siuInvestigator;

    @Column(name = "siu_status")
    private String siuStatus; // 'UNDER_INVESTIGATION' | 'CLEARED' | 'FRAUD_CONFIRMED'

    @Column(name = "deductible")
    private Double deductible;

    @Column(name = "depreciation")
    private Double depreciation;

    // Constructors
    public Claim() {
    }

    // Getters and Setters
    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

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
        return fraudScore;
    }

    public void setFraudScore(Double fraudScore) {
        this.fraudScore = fraudScore;
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

    public SiuInvestigator getSiuInvestigator() {
        return siuInvestigator;
    }

    public void setSiuInvestigator(SiuInvestigator siuInvestigator) {
        this.siuInvestigator = siuInvestigator;
    }

    public String getSiuStatus() {
        return siuStatus;
    }

    public void setSiuStatus(String siuStatus) {
        this.siuStatus = siuStatus;
    }

    public Double getDeductible() {
        return deductible;
    }

    public void setDeductible(Double deductible) {
        this.deductible = deductible;
    }

    public Double getDepreciation() {
        return depreciation;
    }

    public void setDepreciation(Double depreciation) {
        this.depreciation = depreciation;
    }
}
