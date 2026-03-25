package org.hartford.fireinsurance.dto;

public class FraudDistributionResponse {
    private String riskLevel; // LOW, MEDIUM, HIGH
    private Long count;
    private Double percentage;
    private Double totalValue;

    // Default constructor
    public FraudDistributionResponse() {}

    // All-args constructor
    public FraudDistributionResponse(String riskLevel, Long count, Double percentage, Double totalValue) {
        this.riskLevel = riskLevel;
        this.count = count;
        this.percentage = percentage;
        this.totalValue = totalValue;
    }

    // Getters and Setters
    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public Double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(Double totalValue) {
        this.totalValue = totalValue;
    }
}