package org.hartford.fireinsurance.dto;

import java.util.List;

public class FraudAnalysisResponse {

    private Long claimId;
    private Double fraudScore;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private List<FraudRule> ruleBreakdown;
    private String overallAssessment;
    private List<String> suspiciousIndicators;
    private Double confidenceScore;

    // Constructors
    public FraudAnalysisResponse() {
    }

    public FraudAnalysisResponse(Long claimId, Double fraudScore, String riskLevel,
                                 List<FraudRule> ruleBreakdown, String overallAssessment,
                                 List<String> suspiciousIndicators, Double confidenceScore) {
        this.claimId = claimId;
        this.fraudScore = fraudScore;
        this.riskLevel = riskLevel;
        this.ruleBreakdown = ruleBreakdown;
        this.overallAssessment = overallAssessment;
        this.suspiciousIndicators = suspiciousIndicators;
        this.confidenceScore = confidenceScore;
    }

    // Nested class for rule breakdown
    public static class FraudRule {
        private String ruleName;
        private String description;
        private Double weight;
        private Boolean triggered;
        private String impact; // LOW, MEDIUM, HIGH
        private String details;

        public FraudRule() {
        }

        public FraudRule(String ruleName, String description, Double weight, Boolean triggered, String impact, String details) {
            this.ruleName = ruleName;
            this.description = description;
            this.weight = weight;
            this.triggered = triggered;
            this.impact = impact;
            this.details = details;
        }

        // Getters and Setters
        public String getRuleName() {
            return ruleName;
        }

        public void setRuleName(String ruleName) {
            this.ruleName = ruleName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Double getWeight() {
            return weight;
        }

        public void setWeight(Double weight) {
            this.weight = weight;
        }

        public Boolean getTriggered() {
            return triggered;
        }

        public void setTriggered(Boolean triggered) {
            this.triggered = triggered;
        }

        public String getImpact() {
            return impact;
        }

        public void setImpact(String impact) {
            this.impact = impact;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }
    }

    // Main class getters and setters
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

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<FraudRule> getRuleBreakdown() {
        return ruleBreakdown;
    }

    public void setRuleBreakdown(List<FraudRule> ruleBreakdown) {
        this.ruleBreakdown = ruleBreakdown;
    }

    public String getOverallAssessment() {
        return overallAssessment;
    }

    public void setOverallAssessment(String overallAssessment) {
        this.overallAssessment = overallAssessment;
    }

    public List<String> getSuspiciousIndicators() {
        return suspiciousIndicators;
    }

    public void setSuspiciousIndicators(List<String> suspiciousIndicators) {
        this.suspiciousIndicators = suspiciousIndicators;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}