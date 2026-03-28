package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.FraudAnalysisResponse;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.RiskLevel;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FraudAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FraudAnalysisService.class);

    private final ClaimRepository claimRepository;
    private final ClaimService claimService;

    public FraudAnalysisService(ClaimRepository claimRepository, ClaimService claimService) {
        this.claimRepository = claimRepository;
        this.claimService = claimService;
    }

    /**
     * Generate comprehensive fraud analysis for a claim
     */
    public FraudAnalysisResponse generateFraudAnalysis(Long claimId) {
        log.info("Generating dynamic fraud analysis for claim ID: {}", claimId);

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));

        // Generate rule breakdown (single source of truth for both display and score)
        List<FraudAnalysisResponse.FraudRule> ruleBreakdown = generateRuleBreakdown(claim);

        // Sum the weights of all TRIGGERED rules to get the aggregate score
        Double fraudScore = ruleBreakdown.stream()
                .filter(FraudAnalysisResponse.FraudRule::getTriggered)
                .mapToDouble(FraudAnalysisResponse.FraudRule::getWeight)
                .sum();

        // Cap score at 100%
        fraudScore = Math.min(fraudScore, 100.0);

        // Determine risk level
        RiskLevel riskLevel = determineRiskLevelInternal(fraudScore);

        // Update claim with calculated values
        claim.setFraudScore(fraudScore);
        claim.setRiskLevel(riskLevel);
        claimRepository.save(claim);

        // Generate overall assessment
        String overallAssessment = generateOverallAssessment(claim, fraudScore, riskLevel.name());

        // Generate suspicious indicators
        List<String> suspiciousIndicators = generateSuspiciousIndicators(claim);

        // Calculate confidence score
        Double confidenceScore = calculateConfidenceScore(claim, ruleBreakdown);

        FraudAnalysisResponse response = new FraudAnalysisResponse(
                claimId,
                fraudScore,
                riskLevel.name(),
                ruleBreakdown,
                overallAssessment,
                suspiciousIndicators,
                confidenceScore
        );

        log.info("Fraud analysis generated for claim {}: Score={}%, Risk={}", claimId, fraudScore, riskLevel);
        return response;
    }

    /**
     * Determine risk level based on fraud score
     */
    private RiskLevel determineRiskLevelInternal(Double fraudScore) {
        if (fraudScore >= 70) return RiskLevel.HIGH;
        if (fraudScore >= 30) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    /**
     * Generate detailed rule breakdown
     */
    private List<FraudAnalysisResponse.FraudRule> generateRuleBreakdown(Claim claim) {
        List<FraudAnalysisResponse.FraudRule> rules = new ArrayList<>();

        // Rule 1: High Claim Amount (+20 Max)
        double amountWeight = 0;
        String amountImpact = "LOW";
        if (claim.getClaimAmount() != null) {
            if (claim.getClaimAmount() > 1000000) { amountWeight = 20; amountImpact = "HIGH"; }
            else if (claim.getClaimAmount() > 500000) { amountWeight = 15; amountImpact = "MEDIUM"; }
            else if (claim.getClaimAmount() > 200000) { amountWeight = 10; amountImpact = "LOW"; }
        }
        rules.add(new FraudAnalysisResponse.FraudRule(
                "High Claim Amount",
                "Claims exceeding ₹5,00,000 are considered high-risk",
                amountWeight,
                amountWeight > 0,
                amountImpact,
                amountWeight > 0 ? String.format("Claim amount: ₹%,.0f", claim.getClaimAmount()) : "Amount within normal range"
        ));

        // Rule 2: Recent Policy Inception (+15 Max)
        double inceptionWeight = 0;
        String inceptionImpact = "LOW";
        String policyDetails = "Policy inception date not available";
        try {
            if (claim.getSubscription() != null && claim.getSubscription().getStartDate() != null) {
                long daysSinceInception = ChronoUnit.DAYS.between(
                        claim.getSubscription().getStartDate().atStartOfDay(),
                        LocalDateTime.now()
                );
                if (daysSinceInception < 30) { inceptionWeight = 15; inceptionImpact = "HIGH"; }
                else if (daysSinceInception < 90) { inceptionWeight = 10; inceptionImpact = "MEDIUM"; }
                else if (daysSinceInception < 180) { inceptionWeight = 5; inceptionImpact = "LOW"; }
                
                policyDetails = inceptionWeight > 0 ?
                        String.format("Policy started %d days ago", daysSinceInception) :
                        "Policy inception date is acceptable";
            }
        } catch (Exception e) {
            log.warn("Error checking policy inception: {}", e.getMessage());
        }
        rules.add(new FraudAnalysisResponse.FraudRule(
                "Recent Policy Inception",
                "Claims filed within 90 days of policy inception are flagged",
                inceptionWeight,
                inceptionWeight > 0,
                inceptionImpact,
                policyDetails
        ));

        // Rule 3: Multiple Claims Pattern (+25 Max)
        double multipleClaimsWeight = 0;
        String claimsImpact = "LOW";
        String claimsDetails = "Customer claim history not available";
        try {
            if (claim.getSubscription() != null && claim.getSubscription().getCustomer() != null) {
                Customer customer = claim.getSubscription().getCustomer();
                List<Claim> customerClaims = claimService.getClaimsByUsername(customer.getUser().getUsername());

                long recentClaims = customerClaims.stream()
                        .filter(c -> c.getCreatedAt() != null &&
                                   c.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(12)))
                        .count();

                if (recentClaims >= 3) { multipleClaimsWeight = 25; claimsImpact = "HIGH"; }
                else if (recentClaims >= 2) { multipleClaimsWeight = 15; claimsImpact = "MEDIUM"; }
                
                claimsDetails = multipleClaimsWeight > 0 ?
                        String.format("%d claims in past 12 months", recentClaims) :
                        "Normal claim frequency";
            }
        } catch (Exception e) {
            log.warn("Error checking customer claims: {}", e.getMessage());
        }
        rules.add(new FraudAnalysisResponse.FraudRule(
                "Multiple Claims Pattern",
                "Customers with multiple claims in short periods are flagged",
                multipleClaimsWeight,
                multipleClaimsWeight > 0,
                claimsImpact,
                claimsDetails
        ));

        // Rule 4: Suspicious Submission Timing (+10)
        double timingWeight = 0;
        if (claim.getCreatedAt() != null) {
            int hour = claim.getCreatedAt().getHour();
            if (hour < 6 || hour > 22) timingWeight = 10;
        }
        rules.add(new FraudAnalysisResponse.FraudRule(
                "Suspicious Timing",
                "Claims filed at unusual hours (10PM - 6AM) are flagged",
                timingWeight,
                timingWeight > 0,
                "MEDIUM",
                timingWeight > 0 ? "Claim filed during unusual hours" : "Standard filing time"
        ));

        // Rule 5: Documentation Completeness (+10)
        boolean incompleteDocsTriggered = claim.getDescription() == null || claim.getDescription().length() < 100;
        rules.add(new FraudAnalysisResponse.FraudRule(
                "Documentation Completeness",
                "Claims with brief or vague documentation are flagged",
                10.0,
                incompleteDocsTriggered,
                incompleteDocsTriggered ? "MEDIUM" : "LOW",
                incompleteDocsTriggered ? "Insufficient claim description provided" : "Documentation appears complete"
        ));

        return rules;
    }

    /**
     * Determine risk level based on fraud score
     */
    private String determineRiskLevel(Double fraudScore) {
        if (fraudScore >= 70) return "HIGH";
        if (fraudScore >= 30) return "MEDIUM";
        return "LOW";
    }

    /**
     * Generate overall assessment text
     */
    private String generateOverallAssessment(Claim claim, Double fraudScore, String riskLevel) {
        StringBuilder assessment = new StringBuilder();

        assessment.append(String.format("This claim has been assigned a fraud risk score of %.1f%%, ", fraudScore));
        assessment.append(String.format("placing it in the %s risk category. ", riskLevel));

        if (riskLevel.equals("HIGH")) {
            assessment.append("This claim requires immediate SIU investigation due to multiple high-risk indicators. ");
            assessment.append("Recommend thorough documentation review and field investigation before processing.");
        } else if (riskLevel.equals("MEDIUM")) {
            assessment.append("This claim shows moderate risk indicators and should be reviewed carefully. ");
            assessment.append("Consider additional verification of documentation and circumstances.");
        } else {
            assessment.append("This claim appears to have minimal fraud indicators and can proceed through normal processing channels.");
        }

        return assessment.toString();
    }

    /**
     * Generate list of suspicious indicators
     */
    private List<String> generateSuspiciousIndicators(Claim claim) {
        List<String> indicators = new ArrayList<>();

        if (claim.getClaimAmount() != null && claim.getClaimAmount() > 500000) {
            indicators.add("High monetary claim amount");
        }

        try {
            if (claim.getSubscription() != null && claim.getSubscription().getStartDate() != null) {
                long daysSinceInception = ChronoUnit.DAYS.between(
                        claim.getSubscription().getStartDate().atStartOfDay(),
                        LocalDateTime.now()
                );
                if (daysSinceInception < 90) {
                    indicators.add("Claim filed shortly after policy inception");
                }
            }
        } catch (Exception e) {
            log.warn("Error checking policy timing: {}", e.getMessage());
        }

        if (claim.getDescription() == null || claim.getDescription().length() < 100) {
            indicators.add("Insufficient claim documentation");
        }

        if (claim.getCreatedAt() != null) {
            int hour = claim.getCreatedAt().getHour();
            if (hour < 6 || hour > 22) {
                indicators.add("Claim filed during unusual hours");
            }
        }

        // Check for multiple claims
        try {
            if (claim.getSubscription() != null && claim.getSubscription().getCustomer() != null) {
                Customer customer = claim.getSubscription().getCustomer();
                List<Claim> customerClaims = claimService.getClaimsByUsername(customer.getUser().getUsername());
                long count = customerClaims.stream()
                        .filter(c -> c.getCreatedAt() != null &&
                                   c.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(12)))
                        .count();
                if (count >= 2) {
                    indicators.add("Frequent claim filing pattern detected");
                }
            }
        } catch (Exception e) {}

        if (indicators.isEmpty()) {
            indicators.add("No significant suspicious indicators detected");
        }

        return indicators;
    }

    /**
     * Calculate confidence score for the analysis
     */
    private Double calculateConfidenceScore(Claim claim, List<FraudAnalysisResponse.FraudRule> rules) {
        double confidence = 75.0; // Base confidence

        // Increase confidence based on available data
        if (claim.getSubscription() != null && claim.getSubscription().getCustomer() != null) {
            confidence += 10; // Customer data available
        }

        if (claim.getDescription() != null && claim.getDescription().length() > 100) {
            confidence += 10; // Detailed description available
        }

        if (claim.getEstimatedLoss() != null) {
            confidence += 5; // Loss estimation available
        }

        return Math.min(confidence, 95.0); // Cap at 95%
    }
}