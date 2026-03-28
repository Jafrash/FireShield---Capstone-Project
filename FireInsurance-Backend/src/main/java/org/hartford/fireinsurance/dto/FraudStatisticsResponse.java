package org.hartford.fireinsurance.dto;

public class FraudStatisticsResponse {
    private Long totalFraudCases;
    private Long highRiskClaims;
    private Long mediumRiskClaims;
    private Long lowRiskClaims;
    private Long totalSiuInvestigations;
    private Long activeSiuInvestigations;
    private Long completedSiuInvestigations;
    private Long fraudConfirmedCases;
    private Long clearedCases;
    private Double averageFraudScore;
    private Double totalClaimsValue;
    private Double fraudulentClaimsValue;
    private Double fraudPercentage;

    // Default constructor
    public FraudStatisticsResponse() {}

    // All-args constructor
    public FraudStatisticsResponse(Long totalFraudCases, Long highRiskClaims, Long mediumRiskClaims,
                                  Long lowRiskClaims, Long totalSiuInvestigations, Long activeSiuInvestigations,
                                  Long completedSiuInvestigations, Long fraudConfirmedCases, Long clearedCases,
                                  Double averageFraudScore, Double totalClaimsValue, Double fraudulentClaimsValue,
                                  Double fraudPercentage) {
        this.totalFraudCases = totalFraudCases;
        this.highRiskClaims = highRiskClaims;
        this.mediumRiskClaims = mediumRiskClaims;
        this.lowRiskClaims = lowRiskClaims;
        this.totalSiuInvestigations = totalSiuInvestigations;
        this.activeSiuInvestigations = activeSiuInvestigations;
        this.completedSiuInvestigations = completedSiuInvestigations;
        this.fraudConfirmedCases = fraudConfirmedCases;
        this.clearedCases = clearedCases;
        this.averageFraudScore = averageFraudScore;
        this.totalClaimsValue = totalClaimsValue;
        this.fraudulentClaimsValue = fraudulentClaimsValue;
        this.fraudPercentage = fraudPercentage;
    }

    // Getters and Setters
    public Long getTotalFraudCases() {
        return totalFraudCases;
    }

    public void setTotalFraudCases(Long totalFraudCases) {
        this.totalFraudCases = totalFraudCases;
    }

    public Long getHighRiskClaims() {
        return highRiskClaims;
    }

    public void setHighRiskClaims(Long highRiskClaims) {
        this.highRiskClaims = highRiskClaims;
    }

    public Long getMediumRiskClaims() {
        return mediumRiskClaims;
    }

    public void setMediumRiskClaims(Long mediumRiskClaims) {
        this.mediumRiskClaims = mediumRiskClaims;
    }

    public Long getLowRiskClaims() {
        return lowRiskClaims;
    }

    public void setLowRiskClaims(Long lowRiskClaims) {
        this.lowRiskClaims = lowRiskClaims;
    }

    public Long getTotalSiuInvestigations() {
        return totalSiuInvestigations;
    }

    public void setTotalSiuInvestigations(Long totalSiuInvestigations) {
        this.totalSiuInvestigations = totalSiuInvestigations;
    }

    public Long getActiveSiuInvestigations() {
        return activeSiuInvestigations;
    }

    public void setActiveSiuInvestigations(Long activeSiuInvestigations) {
        this.activeSiuInvestigations = activeSiuInvestigations;
    }

    public Long getCompletedSiuInvestigations() {
        return completedSiuInvestigations;
    }

    public void setCompletedSiuInvestigations(Long completedSiuInvestigations) {
        this.completedSiuInvestigations = completedSiuInvestigations;
    }

    public Long getFraudConfirmedCases() {
        return fraudConfirmedCases;
    }

    public void setFraudConfirmedCases(Long fraudConfirmedCases) {
        this.fraudConfirmedCases = fraudConfirmedCases;
    }

    public Long getClearedCases() {
        return clearedCases;
    }

    public void setClearedCases(Long clearedCases) {
        this.clearedCases = clearedCases;
    }

    public Double getAverageFraudScore() {
        return averageFraudScore;
    }

    public void setAverageFraudScore(Double averageFraudScore) {
        this.averageFraudScore = averageFraudScore;
    }

    public Double getTotalClaimsValue() {
        return totalClaimsValue;
    }

    public void setTotalClaimsValue(Double totalClaimsValue) {
        this.totalClaimsValue = totalClaimsValue;
    }

    public Double getFraudulentClaimsValue() {
        return fraudulentClaimsValue;
    }

    public void setFraudulentClaimsValue(Double fraudulentClaimsValue) {
        this.fraudulentClaimsValue = fraudulentClaimsValue;
    }

    public Double getFraudPercentage() {
        return fraudPercentage;
    }

    public void setFraudPercentage(Double fraudPercentage) {
        this.fraudPercentage = fraudPercentage;
    }
}