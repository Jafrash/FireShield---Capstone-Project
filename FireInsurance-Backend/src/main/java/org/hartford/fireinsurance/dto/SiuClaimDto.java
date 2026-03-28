package org.hartford.fireinsurance.dto;

import java.util.List;

/**
 * DTO for SIU (Special Investigation Unit) claim data.
 * Used specifically for fraud investigation dashboard.
 */
public class SiuClaimDto {

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    private String claimId;
    private double fraudScore;
    private String state;
    private String policyNumber;
    private String customerName;
    private Double claimAmount;
    private Priority priority;
    private String assignedDate;
    private String lastActivity;
    private List<String> suspiciousIndicators;

    // Constructors
    public SiuClaimDto() {}

    public SiuClaimDto(String claimId, double fraudScore, String state) {
        this.claimId = claimId;
        this.fraudScore = fraudScore;
        this.state = state;
    }

    // Getters and Setters
    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public double getFraudScore() {
        return fraudScore;
    }

    public void setFraudScore(double fraudScore) {
        this.fraudScore = fraudScore;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Double getClaimAmount() {
        return claimAmount;
    }

    public void setClaimAmount(Double claimAmount) {
        this.claimAmount = claimAmount;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(String assignedDate) {
        this.assignedDate = assignedDate;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(String lastActivity) {
        this.lastActivity = lastActivity;
    }

    public List<String> getSuspiciousIndicators() {
        return suspiciousIndicators;
    }

    public void setSuspiciousIndicators(List<String> suspiciousIndicators) {
        this.suspiciousIndicators = suspiciousIndicators;
    }
}