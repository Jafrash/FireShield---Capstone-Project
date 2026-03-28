package org.hartford.fireinsurance.dto;

public class InspectionRequest {
    private Double assessedRiskScore;
    private String remarks;
    private String inspectionReport;
    private Double recommendedCoverage;
    private Boolean fireSafetyAvailable;
    private Boolean sprinklerSystem;
    private Boolean fireExtinguishers;
    private Double distanceFromFireStation;
    private Double constructionRisk;
    private Double hazardRisk;
    private Double recommendedPremium;
    private String constructionType;
    private String roofType;
    private String occupancyType;
    private String electricalAuditStatus;
    private Boolean hazardousMaterialsPresent;
    private Double adjacentBuildingDistance;
    private String internalProtectionNotes;

    public InspectionRequest() {}

    public Double getAssessedRiskScore() { return assessedRiskScore; }
    public void setAssessedRiskScore(Double assessedRiskScore) { this.assessedRiskScore = assessedRiskScore; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getInspectionReport() { return inspectionReport; }
    public void setInspectionReport(String inspectionReport) { this.inspectionReport = inspectionReport; }

    public Double getRecommendedCoverage() { return recommendedCoverage; }
    public void setRecommendedCoverage(Double recommendedCoverage) { this.recommendedCoverage = recommendedCoverage; }

    public Boolean getFireSafetyAvailable() { return fireSafetyAvailable; }
    public void setFireSafetyAvailable(Boolean fireSafetyAvailable) { this.fireSafetyAvailable = fireSafetyAvailable; }

    public Boolean getSprinklerSystem() { return sprinklerSystem; }
    public void setSprinklerSystem(Boolean sprinklerSystem) { this.sprinklerSystem = sprinklerSystem; }

    public Boolean getFireExtinguishers() { return fireExtinguishers; }
    public void setFireExtinguishers(Boolean fireExtinguishers) { this.fireExtinguishers = fireExtinguishers; }

    public Double getDistanceFromFireStation() { return distanceFromFireStation; }
    public void setDistanceFromFireStation(Double distanceFromFireStation) { this.distanceFromFireStation = distanceFromFireStation; }

    public Double getConstructionRisk() { return constructionRisk; }
    public void setConstructionRisk(Double constructionRisk) { this.constructionRisk = constructionRisk; }

    public Double getHazardRisk() { return hazardRisk; }
    public void setHazardRisk(Double hazardRisk) { this.hazardRisk = hazardRisk; }

    public Double getRecommendedPremium() { return recommendedPremium; }
    public void setRecommendedPremium(Double recommendedPremium) { this.recommendedPremium = recommendedPremium; }

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
