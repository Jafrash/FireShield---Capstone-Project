package org.hartford.fireinsurance.dto;

public class ClaimInspectionRequest {
    private Double estimatedLoss;
    private String damageReport;
    private String causeOfFire;
    private Double salvageValue;
    private String inspectionReport;
    private Double settlementAmount;
    private Double fireBrigadeExpenses;
    private String otherInsuranceDetails;
    private Boolean underInsuranceDetected;
    private Double recommendedSettlement;

    public ClaimInspectionRequest() {}

    public Double getEstimatedLoss() { return estimatedLoss; }
    public void setEstimatedLoss(Double estimatedLoss) { this.estimatedLoss = estimatedLoss; }

    public String getDamageReport() { return damageReport; }
    public void setDamageReport(String damageReport) { this.damageReport = damageReport; }

    public String getCauseOfFire() { return causeOfFire; }
    public void setCauseOfFire(String causeOfFire) { this.causeOfFire = causeOfFire; }

    public Double getSalvageValue() { return salvageValue; }
    public void setSalvageValue(Double salvageValue) { this.salvageValue = salvageValue; }

    public String getInspectionReport() { return inspectionReport; }
    public void setInspectionReport(String inspectionReport) { this.inspectionReport = inspectionReport; }

    public Double getSettlementAmount() { return settlementAmount; }
    public void setSettlementAmount(Double settlementAmount) { this.settlementAmount = settlementAmount; }

    public Double getFireBrigadeExpenses() { return fireBrigadeExpenses; }
    public void setFireBrigadeExpenses(Double fireBrigadeExpenses) { this.fireBrigadeExpenses = fireBrigadeExpenses; }

    public String getOtherInsuranceDetails() { return otherInsuranceDetails; }
    public void setOtherInsuranceDetails(String otherInsuranceDetails) { this.otherInsuranceDetails = otherInsuranceDetails; }

    public Boolean getUnderInsuranceDetected() { return underInsuranceDetected; }
    public void setUnderInsuranceDetected(Boolean underInsuranceDetected) { this.underInsuranceDetected = underInsuranceDetected; }

    public Double getRecommendedSettlement() { return recommendedSettlement; }
    public void setRecommendedSettlement(Double recommendedSettlement) { this.recommendedSettlement = recommendedSettlement; }
}
