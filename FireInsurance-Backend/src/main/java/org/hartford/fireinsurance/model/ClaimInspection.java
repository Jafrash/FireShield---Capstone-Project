package org.hartford.fireinsurance.model;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "claim_inspections")
public class ClaimInspection {

    // Nested ENUM for Claim Inspection Status
    public enum ClaimInspectionStatus {
        ASSIGNED,
        IN_PROGRESS,
        UNDER_REVIEW,
        APPROVED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inspection_id")
    private Long claimInspectionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surveyor_id")
    private Surveyor surveyor;

    private LocalDateTime inspectionDate;

    @Column(name = "damage_estimation")
    private Double estimatedLoss;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String damageReport;

    @Enumerated(EnumType.STRING)
    private ClaimInspectionStatus status;

    @Column(name = "cause_of_fire")
    private String causeOfFire;

    @Column(name = "salvage_value")
    private Double salvageValue;

    @Column(name = "inspection_report", columnDefinition = "TEXT")
    private String inspectionReport;

    @Column(name = "settlement_amount")
    private Double settlementAmount;

    @Column(name = "fire_brigade_expenses")
    private Double fireBrigadeExpenses;

    @Column(name = "other_insurance_details", columnDefinition = "TEXT")
    private String otherInsuranceDetails;

    @Column(name = "under_insurance_detected")
    private Boolean underInsuranceDetected;

    @Column(name = "recommended_settlement")
    private Double recommendedSettlement;

    // Constructors
    public ClaimInspection() {
    }

    public ClaimInspection(Long claimInspectionId, Claim claim, Surveyor surveyor,
                           LocalDateTime inspectionDate, Double estimatedLoss, String damageReport,
                           ClaimInspectionStatus status) {
        this.claimInspectionId = claimInspectionId;
        this.claim = claim;
        this.surveyor = surveyor;
        this.inspectionDate = inspectionDate;
        this.estimatedLoss = estimatedLoss;
        this.damageReport = damageReport;
        this.status = status;
    }

    // Getters and Setters
    public Long getClaimInspectionId() {
        return claimInspectionId;
    }

    public void setClaimInspectionId(Long claimInspectionId) {
        this.claimInspectionId = claimInspectionId;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
    }

    public Surveyor getSurveyor() {
        return surveyor;
    }

    public void setSurveyor(Surveyor surveyor) {
        this.surveyor = surveyor;
    }

    public LocalDateTime getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(LocalDateTime inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public Double getEstimatedLoss() {
        return estimatedLoss;
    }

    public void setEstimatedLoss(Double estimatedLoss) {
        this.estimatedLoss = estimatedLoss;
    }

    public String getDamageReport() {
        return damageReport != null ? damageReport : inspectionReport;
    }

    public void setDamageReport(String damageReport) {
        this.damageReport = damageReport;
        this.inspectionReport = damageReport;
    }

    public ClaimInspectionStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimInspectionStatus status) {
        this.status = status;
    }

    public String getCauseOfFire() {
        return causeOfFire;
    }

    public void setCauseOfFire(String causeOfFire) {
        this.causeOfFire = causeOfFire;
    }

    public Double getSalvageValue() {
        return salvageValue;
    }

    public void setSalvageValue(Double salvageValue) {
        this.salvageValue = salvageValue;
    }

    public String getInspectionReport() {
        return inspectionReport != null ? inspectionReport : damageReport;
    }

    public void setInspectionReport(String inspectionReport) {
        this.inspectionReport = inspectionReport;
        this.damageReport = inspectionReport;
    }

    public Double getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(Double settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public Double getFireBrigadeExpenses() {
        return fireBrigadeExpenses;
    }

    public void setFireBrigadeExpenses(Double fireBrigadeExpenses) {
        this.fireBrigadeExpenses = fireBrigadeExpenses;
    }

    public String getOtherInsuranceDetails() {
        return otherInsuranceDetails;
    }

    public void setOtherInsuranceDetails(String otherInsuranceDetails) {
        this.otherInsuranceDetails = otherInsuranceDetails;
    }

    public Boolean getUnderInsuranceDetected() {
        return underInsuranceDetected;
    }

    public void setUnderInsuranceDetected(Boolean underInsuranceDetected) {
        this.underInsuranceDetected = underInsuranceDetected;
    }

    public Double getRecommendedSettlement() {
        return recommendedSettlement;
    }

    public void setRecommendedSettlement(Double recommendedSettlement) {
        this.recommendedSettlement = recommendedSettlement;
    }
}