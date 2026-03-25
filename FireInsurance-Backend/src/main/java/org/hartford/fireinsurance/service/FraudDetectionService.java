package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.FraudAnalysisResponse;
import org.hartford.fireinsurance.dto.FraudAnalysisResponse.FraudRuleResult;
import org.hartford.fireinsurance.model.Blacklist.BlacklistType;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.Claim.FraudStatus;
import org.hartford.fireinsurance.model.Claim.RiskLevel;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.BlacklistRepository;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.hartford.fireinsurance.repository.PropertyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for fraud detection in insurance claims.
 * Implements rule-based fraud scoring and pattern detection.
 */
@Service
@Transactional
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    // === FRAUD DETECTION THRESHOLDS (Configurable) ===
    private static final double HIGH_CLAIM_AMOUNT_THRESHOLD = 500000.0;  // ₹5,00,000
    private static final int FREQUENT_CLAIMS_COUNT = 3;                   // More than 3 claims
    private static final int FREQUENT_CLAIMS_DAYS = 30;                   // In last 30 days
    private static final int EARLY_CLAIM_DAYS = 15;                       // Within 15 days of policy start
    private static final double AUTO_SIU_THRESHOLD = 70.0;                // Auto-assign to SIU if score >= 70

    // === SCORE CONTRIBUTIONS ===
    private static final int SCORE_HIGH_AMOUNT = 30;
    private static final int SCORE_FREQUENT_CLAIMS = 40;
    private static final int SCORE_EARLY_CLAIM = 25;
    private static final int SCORE_DUPLICATE_ADDRESS = 50;
    private static final int SCORE_BLACKLISTED = 100;

    private final ClaimRepository claimRepository;
    private final PropertyRepository propertyRepository;
    private final BlacklistRepository blacklistRepository;

    public FraudDetectionService(ClaimRepository claimRepository,
                                 PropertyRepository propertyRepository,
                                 BlacklistRepository blacklistRepository) {
        this.claimRepository = claimRepository;
        this.propertyRepository = propertyRepository;
        this.blacklistRepository = blacklistRepository;
    }

    /**
     * Main fraud score calculation method.
     * Evaluates all fraud rules and returns comprehensive analysis.
     */
    public FraudAnalysisResponse calculateFraudScore(Claim claim) {
        log.info("=== FRAUD ANALYSIS - Claim ID: {} ===", claim.getClaimId());

        List<FraudRuleResult> ruleResults = new ArrayList<>();
        int totalScore = 0;

        // Rule 1: High Claim Amount
        FraudRuleResult highAmountResult = checkHighClaimAmount(claim);
        ruleResults.add(highAmountResult);
        if (highAmountResult.getTriggered()) {
            totalScore += highAmountResult.getScoreContribution();
        }

        // Rule 2: Frequent Claims
        FraudRuleResult frequentClaimsResult = checkFrequentClaims(claim);
        ruleResults.add(frequentClaimsResult);
        if (frequentClaimsResult.getTriggered()) {
            totalScore += frequentClaimsResult.getScoreContribution();
        }

        // Rule 3: Early Claim after Policy Start
        FraudRuleResult earlyClaimResult = checkEarlyClaim(claim);
        ruleResults.add(earlyClaimResult);
        if (earlyClaimResult.getTriggered()) {
            totalScore += earlyClaimResult.getScoreContribution();
        }

        // Rule 4: Duplicate Address Detection
        FraudRuleResult duplicateAddressResult = checkDuplicateAddress(claim);
        ruleResults.add(duplicateAddressResult);
        if (duplicateAddressResult.getTriggered()) {
            totalScore += duplicateAddressResult.getScoreContribution();
        }

        // Rule 5: Blacklist Check
        FraudRuleResult blacklistResult = checkBlacklist(claim);
        ruleResults.add(blacklistResult);
        if (blacklistResult.getTriggered()) {
            totalScore += blacklistResult.getScoreContribution();
        }

        // Cap score at 100
        totalScore = Math.min(totalScore, 100);

        // Determine risk level and fraud status
        RiskLevel riskLevel = determineRiskLevel(totalScore);
        FraudStatus fraudStatus = determineFraudStatus(totalScore);

        // Build response
        FraudAnalysisResponse response = new FraudAnalysisResponse();
        response.setClaimId(claim.getClaimId());
        response.setFraudScore((double) totalScore);
        response.setRiskLevel(riskLevel);
        response.setFraudStatus(fraudStatus);
        response.setRuleBreakdown(ruleResults);
        response.setAnalysisTimestamp(LocalDateTime.now());
        response.setRecommendation(generateRecommendation(totalScore, riskLevel));

        log.info("Fraud Analysis Complete - Score: {}, Risk: {}, Status: {}", totalScore, riskLevel, fraudStatus);

        return response;
    }

    /**
     * Apply fraud analysis to claim and persist results.
     */
    public Claim applyFraudAnalysis(Claim claim) {
        FraudAnalysisResponse analysis = calculateFraudScore(claim);

        claim.setFraudScore(analysis.getFraudScore());
        claim.setRiskLevel(analysis.getRiskLevel());
        claim.setFraudStatus(analysis.getFraudStatus());
        claim.setFraudAnalysisTimestamp(LocalDateTime.now());

        return claimRepository.save(claim);
    }

    // === RULE IMPLEMENTATIONS ===

    /**
     * Rule 1: Check if claim amount exceeds threshold (₹5,00,000)
     */
    private FraudRuleResult checkHighClaimAmount(Claim claim) {
        FraudRuleResult result = new FraudRuleResult();
        result.setRuleName("HIGH_CLAIM_AMOUNT");
        result.setRuleDescription("Claim amount exceeds INR " + String.format("%.0f", HIGH_CLAIM_AMOUNT_THRESHOLD));
        result.setScoreContribution(SCORE_HIGH_AMOUNT);

        Double amount = claim.getClaimAmount();
        boolean triggered = amount != null && amount > HIGH_CLAIM_AMOUNT_THRESHOLD;

        result.setTriggered(triggered);
        result.setDetails(triggered
                ? String.format("Claim amount INR %.2f exceeds threshold INR %.0f", amount, HIGH_CLAIM_AMOUNT_THRESHOLD)
                : String.format("Claim amount INR %.2f is within acceptable range", amount != null ? amount : 0.0));

        log.debug("Rule HIGH_CLAIM_AMOUNT: triggered={}, amount={}", triggered, amount);
        return result;
    }

    /**
     * Rule 2: Check for frequent claims (more than 3 claims in last 30 days)
     */
    private FraudRuleResult checkFrequentClaims(Claim claim) {
        FraudRuleResult result = new FraudRuleResult();
        result.setRuleName("FREQUENT_CLAIMS");
        result.setRuleDescription("More than " + FREQUENT_CLAIMS_COUNT + " claims filed in last " + FREQUENT_CLAIMS_DAYS + " days");
        result.setScoreContribution(SCORE_FREQUENT_CLAIMS);

        try {
            Customer customer = claim.getSubscription().getCustomer();
            LocalDateTime sinceDate = LocalDateTime.now().minusDays(FREQUENT_CLAIMS_DAYS);
            long claimCount = claimRepository.countClaimsByCustomerSince(customer.getCustomerId(), sinceDate);

            boolean triggered = claimCount > FREQUENT_CLAIMS_COUNT;
            result.setTriggered(triggered);
            result.setDetails(String.format("Customer has filed %d claims in last %d days", claimCount, FREQUENT_CLAIMS_DAYS));

            log.debug("Rule FREQUENT_CLAIMS: triggered={}, count={}", triggered, claimCount);
        } catch (Exception e) {
            log.warn("Unable to verify claim frequency: {}", e.getMessage());
            result.setTriggered(false);
            result.setDetails("Unable to verify claim frequency");
        }

        return result;
    }

    /**
     * Rule 3: Check if claim was filed within 15 days of policy start
     */
    private FraudRuleResult checkEarlyClaim(Claim claim) {
        FraudRuleResult result = new FraudRuleResult();
        result.setRuleName("EARLY_CLAIM");
        result.setRuleDescription("Claim filed within " + EARLY_CLAIM_DAYS + " days of policy start");
        result.setScoreContribution(SCORE_EARLY_CLAIM);

        try {
            PolicySubscription subscription = claim.getSubscription();
            if (subscription != null && subscription.getStartDate() != null && claim.getCreatedAt() != null) {
                long daysSinceStart = ChronoUnit.DAYS.between(
                        subscription.getStartDate().atStartOfDay(),
                        claim.getCreatedAt()
                );

                boolean triggered = daysSinceStart >= 0 && daysSinceStart <= EARLY_CLAIM_DAYS;
                result.setTriggered(triggered);
                result.setDetails(String.format("Claim filed %d days after policy start date", daysSinceStart));

                log.debug("Rule EARLY_CLAIM: triggered={}, daysSinceStart={}", triggered, daysSinceStart);
            } else {
                result.setTriggered(false);
                result.setDetails("Unable to verify policy start date");
            }
        } catch (Exception e) {
            log.warn("Unable to verify early claim: {}", e.getMessage());
            result.setTriggered(false);
            result.setDetails("Unable to verify early claim: " + e.getMessage());
        }

        return result;
    }

    /**
     * Rule 4: Check if property address is used by multiple customers
     */
    private FraudRuleResult checkDuplicateAddress(Claim claim) {
        FraudRuleResult result = new FraudRuleResult();
        result.setRuleName("DUPLICATE_ADDRESS");
        result.setRuleDescription("Property address registered to multiple customers");
        result.setScoreContribution(SCORE_DUPLICATE_ADDRESS);

        try {
            Property property = claim.getSubscription().getProperty();
            if (property != null && property.getAddress() != null) {
                Customer customer = claim.getSubscription().getCustomer();
                long duplicateCount = propertyRepository.countDuplicateAddresses(
                        property.getAddress(),
                        customer.getCustomerId()
                );

                boolean triggered = duplicateCount > 0;
                result.setTriggered(triggered);
                result.setDetails(triggered
                        ? String.format("Address found in %d other customer record(s)", duplicateCount)
                        : "Address is unique to this customer");

                log.debug("Rule DUPLICATE_ADDRESS: triggered={}, duplicates={}", triggered, duplicateCount);
            } else {
                result.setTriggered(false);
                result.setDetails("Property address not available for verification");
            }
        } catch (Exception e) {
            log.warn("Unable to verify duplicate address: {}", e.getMessage());
            result.setTriggered(false);
            result.setDetails("Unable to verify address uniqueness");
        }

        return result;
    }

    /**
     * Rule 5: Check if user, email, phone, or address is blacklisted
     */
    private FraudRuleResult checkBlacklist(Claim claim) {
        FraudRuleResult result = new FraudRuleResult();
        result.setRuleName("BLACKLISTED_ENTITY");
        result.setRuleDescription("User, email, phone, or address found in fraud blacklist");
        result.setScoreContribution(SCORE_BLACKLISTED);

        List<String> blacklistMatches = new ArrayList<>();

        try {
            Customer customer = claim.getSubscription().getCustomer();
            User user = customer.getUser();

            // Check username (case-insensitive)
            if (user.getUsername() != null &&
                    blacklistRepository.existsActiveByTypeAndValueIgnoreCase(BlacklistType.USER, user.getUsername())) {
                blacklistMatches.add("Username blacklisted");
            }

            // Check email (case-insensitive)
            if (user.getEmail() != null &&
                    blacklistRepository.existsActiveByTypeAndValueIgnoreCase(BlacklistType.EMAIL, user.getEmail())) {
                blacklistMatches.add("Email blacklisted");
            }

            // Check phone (case-insensitive)
            if (user.getPhoneNumber() != null &&
                    blacklistRepository.existsActiveByTypeAndValueIgnoreCase(BlacklistType.PHONE, user.getPhoneNumber())) {
                blacklistMatches.add("Phone number blacklisted");
            }

            // Check property address (case-insensitive)
            Property property = claim.getSubscription().getProperty();
            if (property != null && property.getAddress() != null &&
                    blacklistRepository.existsActiveByTypeAndValueIgnoreCase(BlacklistType.ADDRESS, property.getAddress())) {
                blacklistMatches.add("Property address blacklisted");
            }

            boolean triggered = !blacklistMatches.isEmpty();
            result.setTriggered(triggered);
            result.setDetails(triggered
                    ? "Blacklist matches: " + String.join(", ", blacklistMatches)
                    : "No blacklist matches found");

            log.debug("Rule BLACKLISTED_ENTITY: triggered={}, matches={}", triggered, blacklistMatches);
        } catch (Exception e) {
            log.warn("Unable to verify blacklist status: {}", e.getMessage());
            result.setTriggered(false);
            result.setDetails("Unable to verify blacklist status");
        }

        return result;
    }

    // === HELPER METHODS ===

    /**
     * Determine risk level based on fraud score.
     */
    private RiskLevel determineRiskLevel(int score) {
        if (score >= 70) return RiskLevel.CRITICAL;
        if (score >= 50) return RiskLevel.HIGH;
        if (score >= 25) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    /**
     * Determine initial fraud status based on score.
     */
    private FraudStatus determineFraudStatus(int score) {
        if (score >= AUTO_SIU_THRESHOLD) {
            return FraudStatus.SIU_INVESTIGATION;
        } else if (score >= 50) {
            return FraudStatus.FLAGGED;
        } else if (score >= 25) {
            return FraudStatus.UNDER_REVIEW;
        }
        return FraudStatus.CLEAR;
    }

    /**
     * Generate recommendation based on risk assessment.
     */
    private String generateRecommendation(int score, RiskLevel riskLevel) {
        switch (riskLevel) {
            case CRITICAL:
                return "IMMEDIATE ACTION REQUIRED: Multiple high-risk indicators detected. " +
                       "This claim has been automatically flagged for Special Investigations Unit (SIU) review. " +
                       "Do not process until investigation is complete.";
            case HIGH:
                return "ELEVATED RISK: Significant fraud indicators present. " +
                       "Manual review by senior underwriter recommended before processing. " +
                       "Consider requesting additional documentation.";
            case MEDIUM:
                return "MODERATE RISK: Some indicators warrant attention. " +
                       "Standard enhanced review process recommended. " +
                       "Verify claim details and supporting documents.";
            default:
                return "LOW RISK: No significant fraud indicators detected. " +
                       "Proceed with normal claim processing workflow.";
        }
    }

    // === PATTERN DETECTION METHODS (Public API) ===

    /**
     * Check if a customer has filed more than the threshold number of claims in the specified period.
     */
    public boolean isFrequentClaims(Long customerId) {
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(FREQUENT_CLAIMS_DAYS);
        long count = claimRepository.countClaimsByCustomerSince(customerId, sinceDate);
        return count > FREQUENT_CLAIMS_COUNT;
    }

    /**
     * Check if an address is used by multiple customers (excluding the specified customer).
     */
    public boolean isDuplicateAddress(String address, Long excludeCustomerId) {
        return propertyRepository.countDuplicateAddresses(address, excludeCustomerId) > 0;
    }
}
