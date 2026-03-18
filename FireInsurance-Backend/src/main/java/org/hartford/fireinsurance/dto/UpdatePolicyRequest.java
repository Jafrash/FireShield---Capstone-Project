package org.hartford.fireinsurance.dto;
public class UpdatePolicyRequest {
    private String policyName;
    private String coverageDetails;
    private Double basePremium;
    private Double maxCoverageAmount;
    private Integer durationMonths;
    public UpdatePolicyRequest() {}
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getCoverageDetails() { return coverageDetails; }
    public void setCoverageDetails(String coverageDetails) { this.coverageDetails = coverageDetails; }
    public Double getBasePremium() { return basePremium; }
    public void setBasePremium(Double basePremium) { this.basePremium = basePremium; }
    public Double getMaxCoverageAmount() { return maxCoverageAmount; }
    public void setMaxCoverageAmount(Double maxCoverageAmount) { this.maxCoverageAmount = maxCoverageAmount; }
    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }
}
