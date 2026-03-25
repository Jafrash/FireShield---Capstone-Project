package org.hartford.fireinsurance.dto;

import java.util.List;

/**
 * Response DTO for SIU claims dashboard containing claims data and statistics.
 */
public class SiuClaimsResponse {

    private List<SiuClaimDto> claims;
    private int totalCount;
    private int highPriorityCount;
    private int suspiciousCount;
    private double averageFraudScore;
    private double totalAmount;

    // Constructors
    public SiuClaimsResponse() {}

    public SiuClaimsResponse(List<SiuClaimDto> claims, int totalCount, int highPriorityCount,
                           int suspiciousCount, double averageFraudScore, double totalAmount) {
        this.claims = claims;
        this.totalCount = totalCount;
        this.highPriorityCount = highPriorityCount;
        this.suspiciousCount = suspiciousCount;
        this.averageFraudScore = averageFraudScore;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public List<SiuClaimDto> getClaims() {
        return claims;
    }

    public void setClaims(List<SiuClaimDto> claims) {
        this.claims = claims;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getHighPriorityCount() {
        return highPriorityCount;
    }

    public void setHighPriorityCount(int highPriorityCount) {
        this.highPriorityCount = highPriorityCount;
    }

    public int getSuspiciousCount() {
        return suspiciousCount;
    }

    public void setSuspiciousCount(int suspiciousCount) {
        this.suspiciousCount = suspiciousCount;
    }

    public double getAverageFraudScore() {
        return averageFraudScore;
    }

    public void setAverageFraudScore(double averageFraudScore) {
        this.averageFraudScore = averageFraudScore;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}