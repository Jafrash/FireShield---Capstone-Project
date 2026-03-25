package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.Claim;

import java.time.LocalDateTime;

/**
 * Simple DTO for SIU cases to avoid JSON circular references
 */
public class SiuCaseResponse {
    private Long claimId;
    private String claimNumber;
    private String description;
    private String status;
    private String fraudStatus;
    private Double claimAmount;
    private Double fraudScore;
    private String riskLevel;
    private Long siuInvestigatorId;
    private String investigatorName;
    private LocalDateTime createdAt;
    private String customerName;

    public SiuCaseResponse() {
    }

    public SiuCaseResponse(Claim claim) {
        this.claimId = claim.getClaimId();
        this.claimNumber = claim.getClaimId() != null ? "CLM-" + claim.getClaimId() : null;
        this.description = claim.getDescription();
        this.status = claim.getStatus() != null ? claim.getStatus().toString() : "UNKNOWN";
        this.fraudStatus = claim.getFraudStatus() != null ? claim.getFraudStatus().toString() : "CLEAR";
        this.claimAmount = claim.getClaimAmount();
        this.fraudScore = claim.getFraudScore();
        this.riskLevel = claim.getRiskLevel() != null ? claim.getRiskLevel().toString() : "LOW";
        this.siuInvestigatorId = claim.getSiuInvestigatorId();

        if (claim.getSiuInvestigator() != null && claim.getSiuInvestigator().getUsername() != null) {
            this.investigatorName = claim.getSiuInvestigator().getUsername();
        }

        this.createdAt = claim.getCreatedAt();

        if (claim.getSubscription() != null && claim.getSubscription().getCustomer() != null
            && claim.getSubscription().getCustomer().getUser() != null) {
            this.customerName = claim.getSubscription().getCustomer().getUser().getUsername();
        }
    }

    // Getters and setters
    public Long getClaimId() { return claimId; }
    public void setClaimId(Long claimId) { this.claimId = claimId; }

    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFraudStatus() { return fraudStatus; }
    public void setFraudStatus(String fraudStatus) { this.fraudStatus = fraudStatus; }

    public Double getClaimAmount() { return claimAmount; }
    public void setClaimAmount(Double claimAmount) { this.claimAmount = claimAmount; }

    public Double getFraudScore() { return fraudScore; }
    public void setFraudScore(Double fraudScore) { this.fraudScore = fraudScore; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Long getSiuInvestigatorId() { return siuInvestigatorId; }
    public void setSiuInvestigatorId(Long siuInvestigatorId) { this.siuInvestigatorId = siuInvestigatorId; }

    public String getInvestigatorName() { return investigatorName; }
    public void setInvestigatorName(String investigatorName) { this.investigatorName = investigatorName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
}