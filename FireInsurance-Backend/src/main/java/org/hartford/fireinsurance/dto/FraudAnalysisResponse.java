package org.hartford.fireinsurance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.hartford.fireinsurance.model.Claim.RiskLevel;
import org.hartford.fireinsurance.model.Claim.FraudStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for fraud analysis results.
 * Contains the fraud score, risk level, and detailed breakdown of triggered rules.
 */
public class FraudAnalysisResponse {

    private Long claimId;
    private Double fraudScore;
    private RiskLevel riskLevel;
    private FraudStatus fraudStatus;
    private List<FraudRuleResult> ruleBreakdown;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime analysisTimestamp;
    private String recommendation;

    /**
     * Represents the result of a single fraud detection rule evaluation.
     */
    public static class FraudRuleResult {
        private String ruleName;
        private String ruleDescription;
        private Integer scoreContribution;
        private Boolean triggered;
        private String details;

        public FraudRuleResult() {
        }

        public FraudRuleResult(String ruleName, String ruleDescription, Integer scoreContribution, Boolean triggered, String details) {
            this.ruleName = ruleName;
            this.ruleDescription = ruleDescription;
            this.scoreContribution = scoreContribution;
            this.triggered = triggered;
            this.details = details;
        }

        // Getters and Setters
        public String getRuleName() {
            return ruleName;
        }

        public void setRuleName(String ruleName) {
            this.ruleName = ruleName;
        }

        public String getRuleDescription() {
            return ruleDescription;
        }

        public void setRuleDescription(String ruleDescription) {
            this.ruleDescription = ruleDescription;
        }

        public Integer getScoreContribution() {
            return scoreContribution;
        }

        public void setScoreContribution(Integer scoreContribution) {
            this.scoreContribution = scoreContribution;
        }

        public Boolean getTriggered() {
            return triggered;
        }

        public void setTriggered(Boolean triggered) {
            this.triggered = triggered;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }
    }

    // Constructors
    public FraudAnalysisResponse() {
    }

    // Getters and Setters
    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public Double getFraudScore() {
        return fraudScore;
    }

    public void setFraudScore(Double fraudScore) {
        this.fraudScore = fraudScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public FraudStatus getFraudStatus() {
        return fraudStatus;
    }

    public void setFraudStatus(FraudStatus fraudStatus) {
        this.fraudStatus = fraudStatus;
    }

    public List<FraudRuleResult> getRuleBreakdown() {
        return ruleBreakdown;
    }

    public void setRuleBreakdown(List<FraudRuleResult> ruleBreakdown) {
        this.ruleBreakdown = ruleBreakdown;
    }

    public LocalDateTime getAnalysisTimestamp() {
        return analysisTimestamp;
    }

    public void setAnalysisTimestamp(LocalDateTime analysisTimestamp) {
        this.analysisTimestamp = analysisTimestamp;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
