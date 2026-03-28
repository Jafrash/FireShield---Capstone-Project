package org.hartford.fireinsurance.dto;

public class FraudTrendResponse {
    private String month;
    private Long fraudCases;
    private Long totalClaims;
    private Double fraudPercentage;

    // Default constructor
    public FraudTrendResponse() {}

    // All-args constructor
    public FraudTrendResponse(String month, Long fraudCases, Long totalClaims, Double fraudPercentage) {
        this.month = month;
        this.fraudCases = fraudCases;
        this.totalClaims = totalClaims;
        this.fraudPercentage = fraudPercentage;
    }

    // Getters and Setters
    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Long getFraudCases() {
        return fraudCases;
    }

    public void setFraudCases(Long fraudCases) {
        this.fraudCases = fraudCases;
    }

    public Long getTotalClaims() {
        return totalClaims;
    }

    public void setTotalClaims(Long totalClaims) {
        this.totalClaims = totalClaims;
    }

    public Double getFraudPercentage() {
        return fraudPercentage;
    }

    public void setFraudPercentage(Double fraudPercentage) {
        this.fraudPercentage = fraudPercentage;
    }
}