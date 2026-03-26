package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.Claim;
import java.time.LocalDateTime;

public class ClaimResponse {
    private Long claimId;
    private Long subscriptionId;
    private String description;
    private Double claimAmount;
    private Claim.ClaimStatus status;
    private LocalDateTime createdAt;
    private Double estimatedLoss;
    private Double deductible;
    private Double depreciation;
    private Double settlementAmount;
    private Long underwriterId;
    private String riskLevel;
    private Double fraudScore;

    public ClaimResponse() {
    }

    public ClaimResponse(Long claimId, Long subscriptionId, String description,
            Double claimAmount, Claim.ClaimStatus status, LocalDateTime createdAt,
            Double estimatedLoss, Double deductible, Double depreciation, Double settlementAmount,
            Long underwriterId, String riskLevel, Double fraudScore) {
        this.claimId = claimId;
        this.subscriptionId = subscriptionId;
        this.description = description;
        this.claimAmount = claimAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.estimatedLoss = estimatedLoss;
        this.deductible = deductible;
        this.depreciation = depreciation;
        this.settlementAmount = settlementAmount;
        this.underwriterId = underwriterId;
        this.riskLevel = riskLevel;
        this.fraudScore = fraudScore;
    }
    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Double getFraudScore() {
        return fraudScore;
    }

    public void setFraudScore(Double fraudScore) {
        this.fraudScore = fraudScore;
    }

    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getClaimAmount() {
        return claimAmount;
    }

    public void setClaimAmount(Double claimAmount) {
        this.claimAmount = claimAmount;
    }

    public Claim.ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(Claim.ClaimStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Double getEstimatedLoss() {
        return estimatedLoss;
    }

    public void setEstimatedLoss(Double estimatedLoss) {
        this.estimatedLoss = estimatedLoss;
    }

    public Double getDeductible() {
        return deductible;
    }

    public void setDeductible(Double deductible) {
        this.deductible = deductible;
    }

    public Double getDepreciation() {
        return depreciation;
    }

    public void setDepreciation(Double depreciation) {
        this.depreciation = depreciation;
    }

    public Double getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(Double settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public Long getUnderwriterId() {
        return underwriterId;
    }

    public void setUnderwriterId(Long underwriterId) {
        this.underwriterId = underwriterId;
    }
}
