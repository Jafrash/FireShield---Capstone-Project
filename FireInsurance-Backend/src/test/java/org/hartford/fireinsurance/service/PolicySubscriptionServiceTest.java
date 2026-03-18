package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.SubscribeRequest;
import org.hartford.fireinsurance.model.*;
import org.hartford.fireinsurance.repository.NotificationPreferenceRepository;
import org.hartford.fireinsurance.repository.PolicySubscriptionRepository;
import org.hartford.fireinsurance.repository.UnderwriterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PolicySubscriptionServiceTest {

    @Mock
    private PolicySubscriptionRepository subscriptionRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private PropertyService propertyService;

    @Mock
    private PolicyService policyService;

    @Mock
    private InspectionService inspectionService;

    @Mock
    private SurveyorService surveyorService;

    @Mock
    private UnderwriterRepository underwriterRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Spy
    private PremiumCalculationService premiumCalculationService = new PremiumCalculationService();

    @InjectMocks
    private PolicySubscriptionService service;

    private Customer customer;
    private Property property;
    private Policy policy;
    private PolicySubscription subscription;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setUsername("customer1");

        customer = new Customer();
        customer.setCustomerId(1L);
        customer.setUser(user);

        property = new Property();
        property.setPropertyId(1L);
        property.setCustomer(customer);
        property.setRiskScore(5.0);

        policy = new Policy();
        policy.setPolicyId(1L);
        policy.setPolicyName("Fire Insurance");
        policy.setBasePremium(10000.0);
        policy.setMaxCoverageAmount(10000.0);
        policy.setDurationMonths(12);

        subscription = new PolicySubscription();
        subscription.setSubscriptionId(1L);
        subscription.setCustomer(customer);
        subscription.setProperty(property);
        subscription.setPolicy(policy);
        subscription.setStatus(PolicySubscription.SubscriptionStatus.REQUESTED);
        subscription.setBasePremiumAmount(10000.0);
        subscription.setRequestedCoverage(10000.0);
        subscription.setRenewalCount(0);
        subscription.setClaimFreeYears(0);
    }

    @Test
    void testCalculatePremium_LowRisk() {
        double premium = service.calculatePremium(subscription, 2.0, null);
        assertEquals(8000.0, premium);
    }

    @Test
    void testCalculatePremium_NormalRisk() {
        double premium = service.calculatePremium(subscription, 4.0, null);
        assertEquals(10000.0, premium);
    }

    @Test
    void testCalculatePremium_ModerateRisk() {
        double premium = service.calculatePremium(subscription, 6.0, null);
        assertEquals(12000.0, premium);
    }

    @Test
    void testCalculatePremium_HighRisk() {
        double premium = service.calculatePremium(subscription, 8.0, null);
        assertEquals(15000.0, premium);
    }

    @Test
    void testCalculatePremium_VeryHighRisk() {
        double premium = service.calculatePremium(subscription, 10.0, null);
        assertEquals(20000.0, premium);
    }

    @Test
    void testGetRiskMultiplier() {
        assertEquals(0.8, service.getRiskMultiplier(2.0));
        assertEquals(1.0, service.getRiskMultiplier(4.0));
        assertEquals(1.2, service.getRiskMultiplier(6.0));
        assertEquals(1.5, service.getRiskMultiplier(8.0));
        assertEquals(2.0, service.getRiskMultiplier(10.0));
    }

    @Test
    void testGetRiskCategory() {
        assertEquals("Low Risk", service.getRiskCategory(2.0));
        assertEquals("Normal Risk", service.getRiskCategory(4.0));
        assertEquals("Moderate Risk", service.getRiskCategory(6.0));
        assertEquals("High Risk", service.getRiskCategory(8.0));
        assertEquals("Very High Risk", service.getRiskCategory(10.0));
    }

    @Test
    void testSubscribe_Success() {
        SubscribeRequest request = new SubscribeRequest();
        request.setPropertyId(1L);
        request.setPolicyId(1L);

        when(customerService.getCustomerByUsername("customer1")).thenReturn(customer);
        when(propertyService.getPropertyById(1L)).thenReturn(property);
        when(policyService.getPolicyById(1L)).thenReturn(policy);
        when(subscriptionRepository.findByProperty(property)).thenReturn(new ArrayList<>());
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        PolicySubscription result = service.subscribe("customer1", request);

        assertNotNull(result);
        assertEquals(PolicySubscription.SubscriptionStatus.REQUESTED, result.getStatus());
        assertEquals(10000.0, result.getBasePremiumAmount());
        assertNull(result.getPremiumAmount());
        verify(subscriptionRepository).save(any(PolicySubscription.class));
    }

    @Test
    void testSubscribe_UnauthorizedProperty() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("customer2");

        Customer otherCustomer = new Customer();
        otherCustomer.setCustomerId(2L);
        otherCustomer.setUser(otherUser);

        property.setCustomer(otherCustomer);

        SubscribeRequest request = new SubscribeRequest();
        request.setPropertyId(1L);
        request.setPolicyId(1L);

        when(customerService.getCustomerByUsername("customer1")).thenReturn(customer);
        when(propertyService.getPropertyById(1L)).thenReturn(property);
        when(policyService.getPolicyById(1L)).thenReturn(policy);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.subscribe("customer1", request);
        });

        assertTrue(exception.getMessage().contains("Unauthorized"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void testSubscribe_DuplicateActiveSubscription() {
        PolicySubscription existingSub = new PolicySubscription();
        existingSub.setPolicy(policy);
        existingSub.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);

        SubscribeRequest request = new SubscribeRequest();
        request.setPropertyId(1L);
        request.setPolicyId(1L);

        when(customerService.getCustomerByUsername("customer1")).thenReturn(customer);
        when(propertyService.getPropertyById(1L)).thenReturn(property);
        when(policyService.getPolicyById(1L)).thenReturn(policy);
        when(subscriptionRepository.findByProperty(property)).thenReturn(Arrays.asList(existingSub));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.subscribe("customer1", request);
        });

        assertTrue(exception.getMessage().contains("already exists"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void testAssignSurveyorForInspection_Success() {
        Inspection inspection = new Inspection();
        inspection.setInspectionId(1L);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(inspectionService.assignSurveyor(1L, 1L)).thenReturn(inspection);
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        PolicySubscription result = service.assignSurveyorForInspection(1L, 1L);

        assertNotNull(result);
        assertEquals(PolicySubscription.SubscriptionStatus.PENDING, result.getStatus());
        assertEquals(inspection, result.getPropertyInspection());
        verify(inspectionService).assignSurveyor(1L, 1L);
        verify(subscriptionRepository).save(any(PolicySubscription.class));
    }

    @Test
    void testAssignSurveyorForInspection_InvalidStatus() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.assignSurveyorForInspection(1L, 1L);
        });

        assertTrue(exception.getMessage().contains("REQUESTED"));
        verify(inspectionService, never()).assignSurveyor(anyLong(), anyLong());
    }

    @Test
    void testMarkAsInspected_Success() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.PENDING);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        PolicySubscription result = service.markAsInspected(1L);

        assertNotNull(result);
        assertEquals(PolicySubscription.SubscriptionStatus.INSPECTED, result.getStatus());
        assertEquals(5.0, result.getRiskScore());
        assertEquals(1.2, result.getRiskMultiplier());
        assertEquals(12000.0, result.getPremiumAmount());
        verify(subscriptionRepository).save(any(PolicySubscription.class));
    }

    @Test
    void testMarkAsInspected_NoRiskScore() {
        property.setRiskScore(null);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.markAsInspected(1L);
        });

        assertTrue(exception.getMessage().contains("risk score not available"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void testApproveSubscription_Success() {
        Inspection inspection = new Inspection();
        inspection.setInspectionId(1L);

        subscription.setStatus(PolicySubscription.SubscriptionStatus.INSPECTED);
        subscription.setPremiumAmount(12000.0);
        subscription.setPropertyInspection(inspection);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        PolicySubscription result = service.approveSubscription(1L);

        assertNotNull(result);
        assertEquals(PolicySubscription.SubscriptionStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getStartDate());
        assertNotNull(result.getEndDate());
        assertEquals(LocalDate.now().plusMonths(12), result.getEndDate());
        verify(subscriptionRepository).save(any(PolicySubscription.class));
    }

    @Test
    void testApproveSubscription_InvalidStatus() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.REQUESTED);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.approveSubscription(1L);
        });

        assertTrue(exception.getMessage().contains("INSPECTED"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void testApproveSubscription_NoPremium() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.INSPECTED);
        subscription.setPremiumAmount(null);

        Inspection inspection = new Inspection();
        subscription.setPropertyInspection(inspection);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.approveSubscription(1L);
        });

        assertTrue(exception.getMessage().contains("Premium not calculated"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void testRejectSubscription_Success() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        PolicySubscription result = service.rejectSubscription(1L);

        assertNotNull(result);
        assertEquals(PolicySubscription.SubscriptionStatus.REJECTED, result.getStatus());
        verify(subscriptionRepository).save(any(PolicySubscription.class));
    }

    @Test
    void testRejectSubscription_ActiveSubscription() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.rejectSubscription(1L);
        });

        assertTrue(exception.getMessage().contains("Cannot reject an ACTIVE"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void testCancelSubscription_Success() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        PolicySubscription result = service.cancelSubscription(1L);

        assertNotNull(result);
        assertEquals(PolicySubscription.SubscriptionStatus.CANCELLED, result.getStatus());
        verify(subscriptionRepository).save(any(PolicySubscription.class));
    }

    @Test
    void testGetSubscriptionsByUsername_Success() {
        List<PolicySubscription> subscriptions = Arrays.asList(subscription);

        when(customerService.getCustomerByUsername("customer1")).thenReturn(customer);
        when(subscriptionRepository.findByCustomer(customer)).thenReturn(subscriptions);

        List<PolicySubscription> result = service.getSubscriptionsByUsername("customer1");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(subscriptionRepository).findByCustomer(customer);
    }

    @Test
    void testGetAllSubscriptions_Success() {
        List<PolicySubscription> subscriptions = Arrays.asList(subscription);

        when(subscriptionRepository.findAll()).thenReturn(subscriptions);

        List<PolicySubscription> result = service.getAllSubscriptions();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(subscriptionRepository).findAll();
    }

    @Test
    void testGetSubscriptionById_Success() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        PolicySubscription result = service.getSubscriptionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getSubscriptionId());
        verify(subscriptionRepository).findById(1L);
    }

    @Test
    void testGetSubscriptionById_NotFound() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.getSubscriptionById(1L);
        });

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testUpdateRenewalEligibility_Success() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);
        subscription.setEndDate(LocalDate.now().plusDays(20));
        subscription.setRenewalEligible(false);

        List<PolicySubscription> activeSubscriptions = Arrays.asList(subscription);

        when(subscriptionRepository.findByStatus(PolicySubscription.SubscriptionStatus.ACTIVE)).thenReturn(activeSubscriptions);
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        service.updateRenewalEligibility();

        verify(subscriptionRepository).save(subscription);
        assertTrue(subscription.getRenewalEligible());
    }

    @Test
    void testRenewPolicy_Success() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);
        subscription.setRenewalEligible(true);
        subscription.setEndDate(LocalDate.now().plusDays(20));
        subscription.setPremiumAmount(12000.0);
        subscription.setRiskScore(5.0);
        subscription.setRiskMultiplier(1.2);
        subscription.setClaims(new ArrayList<>());

        Inspection inspection = new Inspection();
        subscription.setPropertyInspection(inspection);

        PolicySubscription renewed = new PolicySubscription();
        renewed.setSubscriptionId(2L);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenAnswer(invocation -> {
            PolicySubscription saved = invocation.getArgument(0);
            if (saved.getSubscriptionId() == null) {
                saved.setSubscriptionId(2L);
            }
            return saved;
        });

        PolicySubscription result = service.renewPolicy(1L);

        assertNotNull(result);
        assertEquals(PolicySubscription.SubscriptionStatus.ACTIVE, result.getStatus());
        assertEquals(1L, result.getPreviousSubscriptionId());
        assertEquals(1, result.getRenewalCount());
        assertEquals(subscription.getEndDate(), result.getStartDate());
        assertEquals(1, result.getClaimFreeYears());
        verify(subscriptionRepository, times(2)).save(any(PolicySubscription.class));
    }

    @Test
    void testRenewPolicy_WithClaims() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);
        subscription.setRenewalEligible(true);
        subscription.setEndDate(LocalDate.now().plusDays(20));
        subscription.setPremiumAmount(12000.0);

        Claim claim = new Claim();
        claim.setStatus(Claim.ClaimStatus.APPROVED);
        subscription.setClaims(Arrays.asList(claim));

        Inspection inspection = new Inspection();
        subscription.setPropertyInspection(inspection);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenAnswer(invocation -> {
            PolicySubscription saved = invocation.getArgument(0);
            if (saved.getSubscriptionId() == null) {
                saved.setSubscriptionId(2L);
            }
            return saved;
        });

        PolicySubscription result = service.renewPolicy(1L);

        assertNotNull(result);
        assertEquals(0, result.getClaimFreeYears());
        assertEquals(0.0, result.getNcbDiscount());
    }

    @Test
    void testRenewPolicy_InvalidStatus() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.CANCELLED);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.renewPolicy(1L);
        });

        assertTrue(exception.getMessage().contains("ACTIVE or EXPIRED"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void testCalculateNCBDiscount() {
        assertEquals(0.0, service.calculateNCBDiscount(0));
        assertEquals(0.10, service.calculateNCBDiscount(1));
        assertEquals(0.20, service.calculateNCBDiscount(2));
        assertEquals(0.25, service.calculateNCBDiscount(3));
        assertEquals(0.35, service.calculateNCBDiscount(4));
        assertEquals(0.50, service.calculateNCBDiscount(5));
        assertEquals(0.50, service.calculateNCBDiscount(10));
    }

    @Test
    void testResetNCBForClaim_Success() {
        subscription.setClaimFreeYears(3);
        subscription.setNcbDiscount(0.25);

        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        service.resetNCBForClaim(1L);

        assertEquals(0, subscription.getClaimFreeYears());
        assertEquals(0.0, subscription.getNcbDiscount());
        assertNotNull(subscription.getLastClaimDate());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void testGetRenewalEligibleSubscriptions_Success() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);
        subscription.setRenewalEligible(true);
        subscription.setEndDate(LocalDate.now().plusDays(20)); // Within 30 days

        PolicySubscription sub2 = new PolicySubscription();
        sub2.setSubscriptionId(2L);
        sub2.setCustomer(customer);
        sub2.setProperty(property);
        sub2.setPolicy(policy);
        sub2.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);
        sub2.setRenewalEligible(false);
        sub2.setEndDate(LocalDate.now().plusDays(60)); // Not within 30 days

        List<PolicySubscription> allSubs = Arrays.asList(subscription, sub2);

        when(customerService.getCustomerByUsername("customer1")).thenReturn(customer);
        when(subscriptionRepository.findByCustomer(customer)).thenReturn(allSubs);

        List<PolicySubscription> result = service.getRenewalEligibleSubscriptions("customer1");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(true, result.get(0).getRenewalEligible());
    }

    @Test
    void testGetNCBBenefitsDescription_NoDiscount() {
        String description = service.getNCBBenefitsDescription(0);

        assertNotNull(description);
        assertTrue(description.contains("No NCB discount"));
        assertTrue(description.contains("10%"));
    }

    @Test
    void testGetNCBBenefitsDescription_WithDiscount() {
        String description = service.getNCBBenefitsDescription(3);

        assertNotNull(description);
        assertTrue(description.contains("25%"));
        assertTrue(description.contains("3 claim-free year"));
    }
}
