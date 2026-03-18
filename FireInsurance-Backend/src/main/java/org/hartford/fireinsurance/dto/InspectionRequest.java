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
}
