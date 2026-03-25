package org.hartford.fireinsurance.domain.event;

import org.hartford.fireinsurance.model.Claim.RiskLevel;

import java.util.List;
import java.util.Map;

/**
 * Domain event fired when fraud analysis is completed for a claim.
 * This event enables automatic routing and escalation based on fraud scores.
 */
public class FraudScoreCalculatedEvent extends DomainEvent {

    private final Double fraudScore;
    private final RiskLevel riskLevel;
    private final Map<String, Object> ruleResults; // Results of each fraud detection rule
    private final String analysisVersion;
    private final List<String> triggeredRules;

    public FraudScoreCalculatedEvent(Long claimId, Double fraudScore, RiskLevel riskLevel,
                                   Map<String, Object> ruleResults, String analysisVersion,
                                   List<String> triggeredRules) {
        super(claimId);
        this.fraudScore = fraudScore;
        this.riskLevel = riskLevel;
        this.ruleResults = ruleResults;
        this.analysisVersion = analysisVersion;
        this.triggeredRules = triggeredRules;
    }

    public Double getFraudScore() {
        return fraudScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public Map<String, Object> getRuleResults() {
        return ruleResults;
    }

    public String getAnalysisVersion() {
        return analysisVersion;
    }

    public List<String> getTriggeredRules() {
        return triggeredRules;
    }

    public Long getClaimId() {
        return getAggregateId();
    }

    @Override
    public String getEventDetails() {
        return String.format("Fraud analysis completed for claim %d. Score: %.2f, Risk Level: %s, Triggered Rules: %s",
            getClaimId(), fraudScore, riskLevel, String.join(", ", triggeredRules));
    }

    /**
     * Determines if this fraud score requires immediate SIU escalation.
     */
    public boolean requiresSiuEscalation() {
        return fraudScore != null && fraudScore >= 70.0;
    }

    /**
     * Determines if this fraud score requires enhanced underwriter review.
     */
    public boolean requiresEnhancedReview() {
        return fraudScore != null && fraudScore >= 50.0 && fraudScore < 70.0;
    }

    /**
     * Checks if blacklist rules were triggered.
     */
    public boolean hasBlacklistViolations() {
        return triggeredRules != null && triggeredRules.stream()
                .anyMatch(rule -> rule.toLowerCase().contains("blacklist"));
    }
}