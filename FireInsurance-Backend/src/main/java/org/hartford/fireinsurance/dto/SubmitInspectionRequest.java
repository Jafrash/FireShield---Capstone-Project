package org.hartford.fireinsurance.dto;
public class SubmitInspectionRequest {
    private String report;
    private Double damageEstimatedAmount;
    public SubmitInspectionRequest() {}
    public String getReport() { return report; }
    public void setReport(String report) { this.report = report; }
    public Double getDamageEstimatedAmount() { return damageEstimatedAmount; }
    public void setDamageEstimatedAmount(Double damageEstimatedAmount) { this.damageEstimatedAmount = damageEstimatedAmount; }
}
