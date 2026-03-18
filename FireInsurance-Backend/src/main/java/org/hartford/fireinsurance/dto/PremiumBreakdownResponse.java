package org.hartford.fireinsurance.dto;

import java.util.List;

public class PremiumBreakdownResponse {
    private Long subscriptionId;
    private String policyName;
    private Double totalPremium;
    private Double monthlyPremium;
    private Integer installmentMonths;
    private Double approvedCoverage;
    private Double requestedCoverage;
    private Double maxCoverageAmount;
    private Double basePremiumReference;
    private Double derivedBaseRate;
    private Double basePremiumForCoverage;
    private Double riskScore;
    private Double baseRiskMultiplier;
    private Double rawRiskFactor;
    private Double finalRiskFactor;
    private Double deductible;
    private List<PremiumBreakdownLineItem> lineItems;

    public PremiumBreakdownResponse() {
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public Double getTotalPremium() {
        return totalPremium;
    }

    public void setTotalPremium(Double totalPremium) {
        this.totalPremium = totalPremium;
    }

    public Double getMonthlyPremium() {
        return monthlyPremium;
    }

    public void setMonthlyPremium(Double monthlyPremium) {
        this.monthlyPremium = monthlyPremium;
    }

    public Integer getInstallmentMonths() {
        return installmentMonths;
    }

    public void setInstallmentMonths(Integer installmentMonths) {
        this.installmentMonths = installmentMonths;
    }

    public Double getApprovedCoverage() {
        return approvedCoverage;
    }

    public void setApprovedCoverage(Double approvedCoverage) {
        this.approvedCoverage = approvedCoverage;
    }

    public Double getRequestedCoverage() {
        return requestedCoverage;
    }

    public void setRequestedCoverage(Double requestedCoverage) {
        this.requestedCoverage = requestedCoverage;
    }

    public Double getMaxCoverageAmount() {
        return maxCoverageAmount;
    }

    public void setMaxCoverageAmount(Double maxCoverageAmount) {
        this.maxCoverageAmount = maxCoverageAmount;
    }

    public Double getBasePremiumReference() {
        return basePremiumReference;
    }

    public void setBasePremiumReference(Double basePremiumReference) {
        this.basePremiumReference = basePremiumReference;
    }

    public Double getDerivedBaseRate() {
        return derivedBaseRate;
    }

    public void setDerivedBaseRate(Double derivedBaseRate) {
        this.derivedBaseRate = derivedBaseRate;
    }

    public Double getBasePremiumForCoverage() {
        return basePremiumForCoverage;
    }

    public void setBasePremiumForCoverage(Double basePremiumForCoverage) {
        this.basePremiumForCoverage = basePremiumForCoverage;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public Double getBaseRiskMultiplier() {
        return baseRiskMultiplier;
    }

    public void setBaseRiskMultiplier(Double baseRiskMultiplier) {
        this.baseRiskMultiplier = baseRiskMultiplier;
    }

    public Double getRawRiskFactor() {
        return rawRiskFactor;
    }

    public void setRawRiskFactor(Double rawRiskFactor) {
        this.rawRiskFactor = rawRiskFactor;
    }

    public Double getFinalRiskFactor() {
        return finalRiskFactor;
    }

    public void setFinalRiskFactor(Double finalRiskFactor) {
        this.finalRiskFactor = finalRiskFactor;
    }

    public Double getDeductible() {
        return deductible;
    }

    public void setDeductible(Double deductible) {
        this.deductible = deductible;
    }

    public List<PremiumBreakdownLineItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<PremiumBreakdownLineItem> lineItems) {
        this.lineItems = lineItems;
    }
}