package org.hartford.fireinsurance.model;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inspections")
public class Inspection {

    // Nested ENUM for Property Inspection Status
    public enum InspectionStatus {
        ASSIGNED,
        IN_PROGRESS,
        COMPLETED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inspectionId;

    @ManyToOne
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne
    @JoinColumn(name = "surveyor_id")
    private Surveyor surveyor;

    private LocalDateTime inspectionDate;

    private Double assessedRiskScore;

    private String remarks;

    @Enumerated(EnumType.STRING)
    private InspectionStatus status;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private PolicySubscription subscription;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "inspection_report", columnDefinition = "TEXT")
    private String inspectionReport;

    @Column(name = "recommended_coverage")
    private Double recommendedCoverage;

    @Column(name = "fire_safety_available")
    private Boolean fireSafetyAvailable;

    @Column(name = "sprinkler_system")
    private Boolean sprinklerSystem;

    @Column(name = "fire_extinguishers")
    private Boolean fireExtinguishers;

    @Column(name = "distance_from_fire_station")
    private Double distanceFromFireStation;

    @Column(name = "construction_risk")
    private Double constructionRisk;

    @Column(name = "hazard_risk")
    private Double hazardRisk;

    @Column(name = "recommended_premium")
    private Double recommendedPremium;

    // Detailed Risk Assessment (COPE - Construction, Occupancy, Protection, Exposure)
    @Column(name = "construction_type")
    private String constructionType;

    @Column(name = "roof_type")
    private String roofType;

    @Column(name = "occupancy_type")
    private String occupancyType;

    @Column(name = "electrical_audit_status")
    private String electricalAuditStatus;

    @Column(name = "hazardous_materials_present")
    private Boolean hazardousMaterialsPresent;

    @Column(name = "adjacent_building_distance")
    private Double adjacentBuildingDistance;

    @Column(name = "internal_protection_notes", columnDefinition = "TEXT")
    private String internalProtectionNotes;

    // Constructors
    public Inspection() {
    }

    // Getters and Setters
    public Long getInspectionId() {
        return inspectionId;
    }

    public void setInspectionId(Long inspectionId) {
        this.inspectionId = inspectionId;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
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

    public Double getAssessedRiskScore() {
        return assessedRiskScore != null ? assessedRiskScore : riskScore;
    }

    public void setAssessedRiskScore(Double assessedRiskScore) {
        this.assessedRiskScore = assessedRiskScore;
        this.riskScore = assessedRiskScore;
    }

    public String getRemarks() {
        return remarks != null ? remarks : inspectionReport;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
        this.inspectionReport = remarks;
    }

    public InspectionStatus getStatus() {
        return status;
    }

    public void setStatus(InspectionStatus status) {
        this.status = status;
    }

    public PolicySubscription getSubscription() {
        return subscription;
    }

    public void setSubscription(PolicySubscription subscription) {
        this.subscription = subscription;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
        this.assessedRiskScore = riskScore;
    }

    public String getInspectionReport() {
        return inspectionReport;
    }

    public void setInspectionReport(String inspectionReport) {
        this.inspectionReport = inspectionReport;
        this.remarks = inspectionReport;
    }

    public Double getRecommendedCoverage() {
        return recommendedCoverage;
    }

    public void setRecommendedCoverage(Double recommendedCoverage) {
        this.recommendedCoverage = recommendedCoverage;
    }

    public Boolean getFireSafetyAvailable() {
        return fireSafetyAvailable;
    }

    public void setFireSafetyAvailable(Boolean fireSafetyAvailable) {
        this.fireSafetyAvailable = fireSafetyAvailable;
    }

    public Boolean getSprinklerSystem() {
        return sprinklerSystem;
    }

    public void setSprinklerSystem(Boolean sprinklerSystem) {
        this.sprinklerSystem = sprinklerSystem;
    }

    public Boolean getFireExtinguishers() {
        return fireExtinguishers;
    }

    public void setFireExtinguishers(Boolean fireExtinguishers) {
        this.fireExtinguishers = fireExtinguishers;
    }

    public Double getDistanceFromFireStation() {
        return distanceFromFireStation;
    }

    public void setDistanceFromFireStation(Double distanceFromFireStation) {
        this.distanceFromFireStation = distanceFromFireStation;
    }

    public Double getConstructionRisk() {
        return constructionRisk;
    }

    public void setConstructionRisk(Double constructionRisk) {
        this.constructionRisk = constructionRisk;
    }

    public Double getHazardRisk() {
        return hazardRisk;
    }

    public void setHazardRisk(Double hazardRisk) {
        this.hazardRisk = hazardRisk;
    }

    public Double getRecommendedPremium() {
        return recommendedPremium;
    }

    public void setRecommendedPremium(Double recommendedPremium) {
        this.recommendedPremium = recommendedPremium;
    }

    // New Handlers
    public String getConstructionType() { return constructionType; }
    public void setConstructionType(String constructionType) { this.constructionType = constructionType; }

    public String getRoofType() { return roofType; }
    public void setRoofType(String roofType) { this.roofType = roofType; }

    public String getOccupancyType() { return occupancyType; }
    public void setOccupancyType(String occupancyType) { this.occupancyType = occupancyType; }

    public String getElectricalAuditStatus() { return electricalAuditStatus; }
    public void setElectricalAuditStatus(String electricalAuditStatus) { this.electricalAuditStatus = electricalAuditStatus; }

    public Boolean getHazardousMaterialsPresent() { return hazardousMaterialsPresent; }
    public void setHazardousMaterialsPresent(Boolean hazardousMaterialsPresent) { this.hazardousMaterialsPresent = hazardousMaterialsPresent; }

    public Double getAdjacentBuildingDistance() { return adjacentBuildingDistance; }
    public void setAdjacentBuildingDistance(Double adjacentBuildingDistance) { this.adjacentBuildingDistance = adjacentBuildingDistance; }

    public String getInternalProtectionNotes() { return internalProtectionNotes; }
    public void setInternalProtectionNotes(String internalProtectionNotes) { this.internalProtectionNotes = internalProtectionNotes; }
}
