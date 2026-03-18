package org.hartford.fireinsurance.dto;

public class PolicyResponse {
    private Long policyId;
    private String policyName;
    private String coverageDetails;
    private Double basePremium;
    private Double maxCoverageAmount;
    private Integer durationMonths;
    private PremiumCalculationFactors premiumCalculationFactors;

    public PolicyResponse() {
    }

    public PolicyResponse(Long policyId, String policyName, String coverageDetails,
                         Double basePremium, Double maxCoverageAmount, Integer durationMonths) {
        this.policyId = policyId;
        this.policyName = policyName;
        this.coverageDetails = coverageDetails;
        this.basePremium = basePremium;
        this.maxCoverageAmount = maxCoverageAmount;
        this.durationMonths = durationMonths;
    }

    public PolicyResponse(Long policyId, String policyName, String coverageDetails,
                         Double basePremium, Double maxCoverageAmount, Integer durationMonths,
                         PremiumCalculationFactors premiumCalculationFactors) {
        this.policyId = policyId;
        this.policyName = policyName;
        this.coverageDetails = coverageDetails;
        this.basePremium = basePremium;
        this.maxCoverageAmount = maxCoverageAmount;
        this.durationMonths = durationMonths;
        this.premiumCalculationFactors = premiumCalculationFactors;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public String getCoverageDetails() {
        return coverageDetails;
    }

    public void setCoverageDetails(String coverageDetails) {
        this.coverageDetails = coverageDetails;
    }

    public Double getBasePremium() {
        return basePremium;
    }

    public void setBasePremium(Double basePremium) {
        this.basePremium = basePremium;
    }

    public Double getMaxCoverageAmount() {
        return maxCoverageAmount;
    }

    public void setMaxCoverageAmount(Double maxCoverageAmount) {
        this.maxCoverageAmount = maxCoverageAmount;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public PremiumCalculationFactors getPremiumCalculationFactors() {
        return premiumCalculationFactors;
    }

    public void setPremiumCalculationFactors(PremiumCalculationFactors premiumCalculationFactors) {
        this.premiumCalculationFactors = premiumCalculationFactors;
    }
}

