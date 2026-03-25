package org.hartford.fireinsurance.service.enhanced;

import org.hartford.fireinsurance.domain.event.DomainEventPublisher;
import org.hartford.fireinsurance.dto.FraudAnalysisResponse;
import org.hartford.fireinsurance.dto.FraudAnalysisResponse.FraudRuleResult;
import org.hartford.fireinsurance.model.Blacklist.BlacklistType;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.Claim.RiskLevel;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.BlacklistRepository;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.hartford.fireinsurance.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for Enhanced Fraud Detection Service.
 *
 * VALIDATION SCENARIOS:
 * 1. High-value claim fraud detection
 * 2. Frequent claims pattern detection
 * 3. Early claim after policy start detection
 * 4. Duplicate address detection
 * 5. Blacklist entity detection
 * 6. Advanced fraud pattern detection
 * 7. Geographic anomaly detection
 * 8. Event-driven architecture integration
 * 9. Risk level determination accuracy
 * 10. Security and performance validation
 */
@ExtendWith(MockitoExtension.class)
class EnhancedFraudDetectionServiceTest {

    private static final Logger log = LoggerFactory.getLogger(EnhancedFraudDetectionServiceTest.class);

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private BlacklistRepository blacklistRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private EnhancedFraudDetectionService fraudDetectionService;

    // Test data
    private Claim lowRiskClaim;
    private Claim highRiskClaim;
    private Claim criticalRiskClaim;
    private Customer testCustomer;
    private PolicySubscription testSubscription;
    private Property testProperty;
    private User testUser;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    /**
     * Test Case 1: Low-risk claim should pass all fraud checks
     */
    @Test
    void testLowRiskClaimAnalysis() {
        log.info("=== Testing Low Risk Claim Analysis ===");

        // Setup: Low-risk claim scenario
        when(claimRepository.findById(1L)).thenReturn(Optional.of(lowRiskClaim));
        when(claimRepository.countClaimsByCustomerSince(anyLong(), any())).thenReturn(1L);
        when(claimRepository.findClaimsByCustomerSince(anyLong(), any())).thenReturn(List.of(lowRiskClaim));
        when(propertyRepository.countDuplicateAddresses(anyString(), anyLong())).thenReturn(0L);
        when(blacklistRepository.existsActiveByTypeAndValueIgnoreCase(any(), anyString())).thenReturn(false);
        when(claimRepository.countClaimsWithSimilarAmount(anyDouble(), anyDouble(), any())).thenReturn(1L);
        when(claimRepository.countClaimsNearTime(any(), any(), any())).thenReturn(1L);
        when(claimRepository.countClaimsInPostalArea(anyString(), any())).thenReturn(5L);

        // Execute: Fraud analysis
        FraudAnalysisResponse response = fraudDetectionService.analyzeClaim(1L);

        // Verify: Low risk results
        assertNotNull(response, "Fraud analysis response should not be null");
        assertEquals(1L, response.getClaimId(), "Claim ID should match");
        assertTrue(response.getFraudScore() < 50.0,
            "Low risk claim should have fraud score < 50, got: " + response.getFraudScore());
        assertEquals(RiskLevel.LOW, response.getRiskLevel(), "Risk level should be LOW");
        assertNotNull(response.getRecommendation(), "Recommendation should be provided");
        assertTrue(response.getRecommendation().contains("LOW RISK"), "Should contain low risk recommendation");

        // Verify: Event publishing
        verify(eventPublisher, times(1)).publish(any());
        verify(claimRepository, times(1)).save(any(Claim.class));

        log.info("✓ Low risk claim analysis completed successfully");
    }

    /**
     * Test Case 2: High-value claim should trigger fraud detection
     */
    @Test
    void testHighValueClaimDetection() {
        log.info("=== Testing High Value Claim Detection ===");

        // Setup: High-value claim scenario
        when(claimRepository.findById(2L)).thenReturn(Optional.of(highRiskClaim));
        when(claimRepository.countClaimsByCustomerSince(anyLong(), any())).thenReturn(1L);
        when(claimRepository.findClaimsByCustomerSince(anyLong(), any())).thenReturn(List.of(highRiskClaim));
        when(propertyRepository.countDuplicateAddresses(anyString(), anyLong())).thenReturn(0L);
        when(blacklistRepository.existsActiveByTypeAndValueIgnoreCase(any(), anyString())).thenReturn(false);
        when(claimRepository.countClaimsWithSimilarAmount(anyDouble(), anyDouble(), any())).thenReturn(1L);
        when(claimRepository.countClaimsNearTime(any(), any(), any())).thenReturn(1L);
        when(claimRepository.countClaimsInPostalArea(anyString(), any())).thenReturn(5L);

        // Execute: Fraud analysis
        FraudAnalysisResponse response = fraudDetectionService.analyzeClaim(2L);

        // Verify: High amount rule triggered
        assertNotNull(response, "Fraud analysis response should not be null");
        assertTrue(response.getFraudScore() >= 30.0,
            "High amount should contribute at least 30 points, got: " + response.getFraudScore());
        assertEquals(RiskLevel.MEDIUM, response.getRiskLevel(), "Risk level should be MEDIUM or higher");

        // Verify: Rule breakdown
        List<FraudRuleResult> rules = response.getRuleBreakdown();
        assertTrue(rules.stream().anyMatch(r ->
            r.getRuleName().equals("HIGH_CLAIM_AMOUNT") && r.getTriggered()),
            "HIGH_CLAIM_AMOUNT rule should be triggered");

        assertTrue(response.getRecommendation().contains("MODERATE RISK") ||
                  response.getRecommendation().contains("HIGH RISK"),
                  "Should contain elevated risk recommendation");

        log.info("✓ High value claim detection completed successfully");
    }

    /**
     * Test Case 3: Critical risk claim with multiple fraud indicators
     */
    @Test
    void testCriticalRiskClaimWithMultipleIndicators() {
        log.info("=== Testing Critical Risk Claim Analysis ===");

        // Setup: Critical risk scenario - multiple fraud indicators
        when(claimRepository.findById(3L)).thenReturn(Optional.of(criticalRiskClaim));
        when(claimRepository.countClaimsByCustomerSince(anyLong(), any())).thenReturn(5L); // Frequent claims
        when(claimRepository.findClaimsByCustomerSince(anyLong(), any())).thenReturn(
            List.of(criticalRiskClaim, criticalRiskClaim, criticalRiskClaim, criticalRiskClaim)); // Multiple high-value claims
        when(propertyRepository.countDuplicateAddresses(anyString(), anyLong())).thenReturn(2L); // Duplicate address
        when(blacklistRepository.existsActiveByTypeAndValueIgnoreCase(BlacklistType.EMAIL, "fraud@test.com")).thenReturn(true); // Blacklisted
        when(blacklistRepository.existsActiveByTypeAndValueIgnoreCase(any(), anyString())).thenReturn(false);
        when(blacklistRepository.existsActiveByTypeAndValueIgnoreCase(BlacklistType.EMAIL, anyString())).thenReturn(true);
        when(claimRepository.countClaimsWithSimilarAmount(anyDouble(), anyDouble(), any())).thenReturn(4L); // Pattern detected
        when(claimRepository.countClaimsNearTime(any(), any(), any())).thenReturn(5L); // Coordinated timing
        when(claimRepository.countClaimsInPostalArea(anyString(), any())).thenReturn(15L); // High density area

        // Execute: Fraud analysis
        FraudAnalysisResponse response = fraudDetectionService.analyzeClaim(3L);

        // Verify: Critical risk results
        assertNotNull(response, "Fraud analysis response should not be null");
        assertTrue(response.getFraudScore() >= 70.0,
            "Critical risk claim should have fraud score >= 70, got: " + response.getFraudScore());
        assertEquals(RiskLevel.CRITICAL, response.getRiskLevel(), "Risk level should be CRITICAL");
        assertTrue(response.getRecommendation().contains("CRITICAL RISK"), "Should contain critical risk recommendation");

        // Verify: Multiple rules triggered
        List<FraudRuleResult> rules = response.getRuleBreakdown();
        long triggeredRules = rules.stream().mapToLong(r -> r.getTriggered() ? 1 : 0).sum();
        assertTrue(triggeredRules >= 3, "Should trigger multiple fraud rules, triggered: " + triggeredRules);

        // Verify: Specific high-risk rules
        assertTrue(rules.stream().anyMatch(r ->
            r.getRuleName().equals("BLACKLISTED_ENTITY") && r.getTriggered()),
            "BLACKLISTED_ENTITY rule should be triggered");

        assertTrue(rules.stream().anyMatch(r ->
            r.getRuleName().equals("FREQUENT_CLAIMS") && r.getTriggered()),
            "FREQUENT_CLAIMS rule should be triggered");

        log.info("✓ Critical risk claim analysis completed successfully");
    }

    /**
     * Test Case 4: Early claim detection after policy start
     */
    @Test
    void testEarlyClaimDetection() {
        log.info("=== Testing Early Claim Detection ===");

        // Setup: Early claim scenario (filed 5 days after policy start)
        Claim earlyClaim = createTestClaim(4L, 100000.0, LocalDateTime.now().minusDays(5));
        PolicySubscription earlySubscription = new PolicySubscription();
        earlySubscription.setStartDate(LocalDate.now().minusDays(5));
        earlySubscription.setCustomer(testCustomer);
        earlySubscription.setProperty(testProperty);
        earlyClaim.setSubscription(earlySubscription);

        when(claimRepository.findById(4L)).thenReturn(Optional.of(earlyClaim));
        when(claimRepository.countClaimsByCustomerSince(anyLong(), any())).thenReturn(1L);
        when(claimRepository.findClaimsByCustomerSince(anyLong(), any())).thenReturn(List.of(earlyClaim));
        when(propertyRepository.countDuplicateAddresses(anyString(), anyLong())).thenReturn(0L);
        when(blacklistRepository.existsActiveByTypeAndValueIgnoreCase(any(), anyString())).thenReturn(false);
        when(claimRepository.countClaimsWithSimilarAmount(anyDouble(), anyDouble(), any())).thenReturn(1L);
        when(claimRepository.countClaimsNearTime(any(), any(), any())).thenReturn(1L);
        when(claimRepository.countClaimsInPostalArea(anyString(), any())).thenReturn(5L);

        // Execute: Fraud analysis
        FraudAnalysisResponse response = fraudDetectionService.analyzeClaim(4L);

        // Verify: Early claim rule triggered
        List<FraudRuleResult> rules = response.getRuleBreakdown();
        assertTrue(rules.stream().anyMatch(r ->
            r.getRuleName().equals("EARLY_CLAIM") && r.getTriggered()),
            "EARLY_CLAIM rule should be triggered for claim filed 5 days after policy start");

        assertTrue(response.getFraudScore() >= 25.0,
            "Early claim should contribute at least 25 points to fraud score");

        log.info("✓ Early claim detection completed successfully");
    }

    /**
     * Test Case 5: Fraud pattern detection for organized fraud
     */
    @Test
    void testFraudPatternDetection() {
        log.info("=== Testing Fraud Pattern Detection ===");

        // Setup: Pattern detection scenario
        when(claimRepository.findById(1L)).thenReturn(Optional.of(lowRiskClaim));
        when(claimRepository.countClaimsByCustomerSince(anyLong(), any())).thenReturn(1L);
        when(claimRepository.findClaimsByCustomerSince(anyLong(), any())).thenReturn(List.of(lowRiskClaim));
        when(propertyRepository.countDuplicateAddresses(anyString(), anyLong())).thenReturn(0L);
        when(blacklistRepository.existsActiveByTypeAndValueIgnoreCase(any(), anyString())).thenReturn(false);

        // Pattern detection triggers
        when(claimRepository.countClaimsWithSimilarAmount(anyDouble(), anyDouble(), any())).thenReturn(4L); // Identical amounts
        when(claimRepository.countClaimsNearTime(any(), any(), any())).thenReturn(5L); // Coordinated timing
        when(claimRepository.countClaimsInPostalArea(anyString(), any())).thenReturn(5L);

        // Execute: Fraud analysis
        FraudAnalysisResponse response = fraudDetectionService.analyzeClaim(1L);

        // Verify: Pattern detection triggered
        List<FraudRuleResult> rules = response.getRuleBreakdown();
        assertTrue(rules.stream().anyMatch(r ->
            r.getRuleName().equals("FRAUD_PATTERNS") && r.getTriggered()),
            "FRAUD_PATTERNS rule should be triggered");

        assertTrue(response.getFraudScore() >= 60.0,
            "Pattern detection should significantly increase fraud score");

        log.info("✓ Fraud pattern detection completed successfully");
    }

    /**
     * Test Case 6: Public API methods validation
     */
    @Test
    void testPublicApiMethods() {
        log.info("=== Testing Public API Methods ===");

        // Test SIU escalation requirement
        assertTrue(fraudDetectionService.requiresSiuEscalation(75.0),
            "Score of 75 should require SIU escalation");
        assertFalse(fraudDetectionService.requiresSiuEscalation(65.0),
            "Score of 65 should not require SIU escalation");
        assertFalse(fraudDetectionService.requiresSiuEscalation(null),
            "Null score should not require SIU escalation");

        // Test enhanced review requirement
        assertTrue(fraudDetectionService.requiresEnhancedReview(55.0),
            "Score of 55 should require enhanced review");
        assertFalse(fraudDetectionService.requiresEnhancedReview(45.0),
            "Score of 45 should not require enhanced review");
        assertFalse(fraudDetectionService.requiresEnhancedReview(75.0),
            "Score of 75 should require SIU, not just enhanced review");

        log.info("✓ Public API methods validation completed successfully");
    }

    /**
     * Test Case 7: Exception handling and resilience
     */
    @Test
    void testExceptionHandlingResilience() {
        log.info("=== Testing Exception Handling and Resilience ===");

        // Setup: Exception scenarios
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        // Verify: Proper exception handling
        assertThrows(IllegalArgumentException.class, () ->
            fraudDetectionService.analyzeClaim(999L),
            "Should throw IllegalArgumentException for non-existent claim");

        // Test database failure resilience
        when(claimRepository.findById(1L)).thenReturn(Optional.of(lowRiskClaim));
        when(claimRepository.countClaimsByCustomerSince(anyLong(), any()))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Should handle gracefully and still provide analysis
        FraudAnalysisResponse response = fraudDetectionService.analyzeClaim(1L);
        assertNotNull(response, "Should still return response even with partial failures");

        log.info("✓ Exception handling and resilience testing completed successfully");
    }

    /**
     * Test Case 8: Geographic anomaly detection
     */
    @Test
    void testGeographicAnomalyDetection() {
        log.info("=== Testing Geographic Anomaly Detection ===");

        // Setup: High-risk geographic area
        Property riskProperty = new Property();
        riskProperty.setAddress("123 Industrial Area, High Crime Zone, Mumbai 400001");
        testSubscription.setProperty(riskProperty);
        lowRiskClaim.setSubscription(testSubscription);

        when(claimRepository.findById(1L)).thenReturn(Optional.of(lowRiskClaim));
        when(claimRepository.countClaimsByCustomerSince(anyLong(), any())).thenReturn(1L);
        when(claimRepository.findClaimsByCustomerSince(anyLong(), any())).thenReturn(List.of(lowRiskClaim));
        when(propertyRepository.countDuplicateAddresses(anyString(), anyLong())).thenReturn(0L);
        when(blacklistRepository.existsActiveByTypeAndValueIgnoreCase(any(), anyString())).thenReturn(false);
        when(claimRepository.countClaimsWithSimilarAmount(anyDouble(), anyDouble(), any())).thenReturn(1L);
        when(claimRepository.countClaimsNearTime(any(), any(), any())).thenReturn(1L);
        when(claimRepository.countClaimsInPostalArea(eq("400"), any())).thenReturn(12L); // High density

        // Execute: Fraud analysis
        FraudAnalysisResponse response = fraudDetectionService.analyzeClaim(1L);

        // Verify: Geographic rule triggered
        List<FraudRuleResult> rules = response.getRuleBreakdown();
        assertTrue(rules.stream().anyMatch(r ->
            r.getRuleName().equals("GEOGRAPHIC_ANOMALY") && r.getTriggered()),
            "GEOGRAPHIC_ANOMALY rule should be triggered for high-risk area");

        log.info("✓ Geographic anomaly detection completed successfully");
    }

    // Helper methods for test data setup

    private void setupTestData() {
        // Setup test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPhoneNumber("9876543210");

        // Setup test customer
        testCustomer = new Customer();
        testCustomer.setCustomerId(1L);
        testCustomer.setUser(testUser);

        // Setup test property
        testProperty = new Property();
        testProperty.setPropertyId(1L);
        testProperty.setAddress("123 Test Street, Mumbai 400001");

        // Setup test subscription
        testSubscription = new PolicySubscription();
        testSubscription.setSubscriptionId(1L);
        testSubscription.setCustomer(testCustomer);
        testSubscription.setProperty(testProperty);
        testSubscription.setStartDate(LocalDate.now().minusDays(30)); // Policy started 30 days ago

        // Setup test claims
        lowRiskClaim = createTestClaim(1L, 50000.0, LocalDateTime.now());
        highRiskClaim = createTestClaim(2L, 600000.0, LocalDateTime.now()); // High amount
        criticalRiskClaim = createTestClaim(3L, 800000.0, LocalDateTime.now()); // Very high amount

        // Setup critical risk user with blacklisted email
        User criticalUser = new User();
        criticalUser.setUserId(3L);
        criticalUser.setUsername("frauduser");
        criticalUser.setEmail("fraud@test.com"); // This will be blacklisted
        criticalUser.setPhoneNumber("1234567890");

        Customer criticalCustomer = new Customer();
        criticalCustomer.setCustomerId(3L);
        criticalCustomer.setUser(criticalUser);

        PolicySubscription criticalSubscription = new PolicySubscription();
        criticalSubscription.setSubscriptionId(3L);
        criticalSubscription.setCustomer(criticalCustomer);
        criticalSubscription.setProperty(testProperty);
        criticalSubscription.setStartDate(LocalDate.now().minusDays(30));

        criticalRiskClaim.setSubscription(criticalSubscription);
    }

    private Claim createTestClaim(Long claimId, Double amount, LocalDateTime createdAt) {
        Claim claim = new Claim();
        claim.setClaimId(claimId);
        claim.setClaimAmount(amount);
        claim.setCreatedAt(createdAt);
        claim.setSubscription(testSubscription);
        claim.setClaimDescription("Test claim for fraud analysis");
        return claim;
    }
}