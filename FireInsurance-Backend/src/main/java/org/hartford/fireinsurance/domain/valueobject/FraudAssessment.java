package org.hartford.fireinsurance.domain.valueobject;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

/**
 * Value object representing fraud assessment information for a claim.
 * This encapsulates all fraud-related data and business logic.
 */
@Embeddable
public class FraudAssessment {

    public enum RiskLevel {
        NEGLIGIBLE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    @Column(name = "fraud_score")
    private Double fraudScore;

    @Column(name = "risk_level")
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(name = "fraud_analysis_timestamp")
    private LocalDateTime lastAnalyzed;

    @Column(name = "analysis_details", columnDefinition = "TEXT")
    private String analysisDetails; // JSON breakdown of rule results

    @Column(name = "analysis_version")
    private String analysisVersion;

    // Default constructor for JPA
    public FraudAssessment() {
    }

    // Private constructor for builder
    private FraudAssessment(Double fraudScore, RiskLevel riskLevel, String analysisDetails,
                           String analysisVersion, LocalDateTime lastAnalyzed) {
        this.fraudScore = fraudScore;
        this.riskLevel = riskLevel;
        this.analysisDetails = analysisDetails;
        this.analysisVersion = analysisVersion;
        this.lastAnalyzed = lastAnalyzed != null ? lastAnalyzed : LocalDateTime.now();
    }

    // Legacy constructor
    public FraudAssessment(Double fraudScore, RiskLevel riskLevel, String analysisDetails,
                          String analysisVersion) {
        this(fraudScore, riskLevel, analysisDetails, analysisVersion, LocalDateTime.now());
    }

    /**
     * Business rule: Claims with fraud score >= 70 require immediate SIU investigation.
     */
    public boolean requiresSiuInvestigation() {
        return fraudScore != null && fraudScore >= 70.0;
    }

    /**
     * Business rule: Claims with fraud score 50-69 require enhanced underwriter review.
     */
    public boolean requiresEnhancedReview() {
        return fraudScore != null && fraudScore >= 50.0 && fraudScore < 70.0;
    }

    /**
     * Business rule: Claims with fraud score >= 25 should have visible fraud warnings.
     */
    public boolean hasSignificantFraudRisk() {
        return fraudScore != null && fraudScore >= 25.0;
    }

    /**
     * Updates the fraud assessment with new analysis results.
     */
    public void updateAssessment(Double newFraudScore, RiskLevel newRiskLevel,
                               String newAnalysisDetails, String newAnalysisVersion) {
        this.fraudScore = newFraudScore;
        this.riskLevel = newRiskLevel;
        this.analysisDetails = newAnalysisDetails;
        this.analysisVersion = newAnalysisVersion;
        this.lastAnalyzed = LocalDateTime.now();
    }

    /**
     * Checks if this assessment is recent (within last 24 hours).
     */
    public boolean isRecent() {
        return lastAnalyzed != null && lastAnalyzed.isAfter(LocalDateTime.now().minusHours(24));
    }

    // Builder pattern implementation
    public static FraudAssessmentBuilder builder() {
        return new FraudAssessmentBuilder();
    }

    public static class FraudAssessmentBuilder {
        private Double fraudScore;
        private RiskLevel riskLevel;
        private String analysisDetails;
        private String analysisVersion;
        private LocalDateTime lastAnalyzed;

        private FraudAssessmentBuilder() {}

        public FraudAssessmentBuilder fraudScore(Double fraudScore) {
            this.fraudScore = fraudScore;
            return this;
        }

        public FraudAssessmentBuilder riskLevel(RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public FraudAssessmentBuilder analysisDetails(String analysisDetails) {
            this.analysisDetails = analysisDetails;
            return this;
        }

        public FraudAssessmentBuilder analysisVersion(String analysisVersion) {
            this.analysisVersion = analysisVersion;
            return this;
        }

        public FraudAssessmentBuilder lastAnalyzed(LocalDateTime lastAnalyzed) {
            this.lastAnalyzed = lastAnalyzed;
            return this;
        }

        public FraudAssessment build() {
            return new FraudAssessment(fraudScore, riskLevel, analysisDetails,
                                     analysisVersion, lastAnalyzed);
        }
    }

    // Getters
    public Double getFraudScore() {
        return fraudScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public LocalDateTime getLastAnalyzed() {
        return lastAnalyzed;
    }

    public String getAnalysisDetails() {
        return analysisDetails;
    }

    public String getAnalysisVersion() {
        return analysisVersion;
    }

    @Override
    public String toString() {
        return String.format("FraudAssessment{score=%.2f, level=%s, analyzed=%s}",
            fraudScore, riskLevel, lastAnalyzed);
    }
}