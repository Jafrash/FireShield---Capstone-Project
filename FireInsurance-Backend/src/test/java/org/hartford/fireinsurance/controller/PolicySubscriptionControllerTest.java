package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.SubscribeRequest;
import org.hartford.fireinsurance.dto.SubscriptionResponse;
import org.hartford.fireinsurance.model.*;
import org.hartford.fireinsurance.service.PolicySubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PolicySubscriptionControllerTest {

    @Mock
    private PolicySubscriptionService subscriptionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PolicySubscriptionController controller;

    private PolicySubscription subscription;
    private Customer customer;
    private Property property;
    private Policy policy;
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

        policy = new Policy();
        policy.setPolicyId(1L);
        policy.setPolicyName("Fire Insurance");
        policy.setBasePremium(10000.0);

        subscription = new PolicySubscription();
        subscription.setSubscriptionId(1L);
        subscription.setCustomer(customer);
        subscription.setProperty(property);
        subscription.setPolicy(policy);
        subscription.setStatus(PolicySubscription.SubscriptionStatus.REQUESTED);
        subscription.setBasePremiumAmount(10000.0);
    }

    @Test
    void testSubscribe_Success() {
        SubscribeRequest request = new SubscribeRequest();
        request.setPropertyId(1L);
        request.setPolicyId(1L);

        when(authentication.getName()).thenReturn("customer1");
        when(subscriptionService.subscribe(eq("customer1"), any(SubscribeRequest.class))).thenReturn(subscription);

        ResponseEntity<SubscriptionResponse> response = controller.subscribe(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getSubscriptionId());
        verify(subscriptionService).subscribe(eq("customer1"), any(SubscribeRequest.class));
    }

    @Test
    void testGetMySubscriptions_Success() {
        PolicySubscription sub2 = new PolicySubscription();
        sub2.setSubscriptionId(2L);
        sub2.setCustomer(customer);
        sub2.setProperty(property);
        sub2.setPolicy(policy);
        sub2.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);

        List<PolicySubscription> subscriptions = Arrays.asList(subscription, sub2);

        when(authentication.getName()).thenReturn("customer1");
        when(subscriptionService.getSubscriptionsByUsername("customer1")).thenReturn(subscriptions);

        ResponseEntity<List<SubscriptionResponse>> response = controller.getMySubscriptions(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(subscriptionService).getSubscriptionsByUsername("customer1");
    }

    @Test
    void testGetAllSubscriptions_Success() {
        List<PolicySubscription> subscriptions = Arrays.asList(subscription);

        when(subscriptionService.getAllSubscriptions()).thenReturn(subscriptions);

        ResponseEntity<List<SubscriptionResponse>> response = controller.getAllSubscriptions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(subscriptionService).getAllSubscriptions();
    }

    @Test
    void testCancelSubscription_Success() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.CANCELLED);

        when(subscriptionService.cancelSubscription(1L)).thenReturn(subscription);

        ResponseEntity<SubscriptionResponse> response = controller.cancelSubscription(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PolicySubscription.SubscriptionStatus.CANCELLED, response.getBody().getStatus());
        verify(subscriptionService).cancelSubscription(1L);
    }

    @Test
    void testApproveSubscription_Success() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.INSPECTED);
        subscription.setPremiumAmount(12000.0);
        subscription.setRiskScore(5.0);
        subscription.setRiskMultiplier(1.2);

        PolicySubscription approved = new PolicySubscription();
        approved.setSubscriptionId(1L);
        approved.setCustomer(customer);
        approved.setProperty(property);
        approved.setPolicy(policy);
        approved.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);
        approved.setStartDate(LocalDate.now());
        approved.setEndDate(LocalDate.now().plusMonths(12));
        approved.setPremiumAmount(12000.0);

        when(subscriptionService.approveSubscription(1L)).thenReturn(approved);

        ResponseEntity<SubscriptionResponse> response = controller.approveSubscription(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PolicySubscription.SubscriptionStatus.ACTIVE, response.getBody().getStatus());
        verify(subscriptionService).approveSubscription(1L);
    }

    @Test
    void testRejectSubscription_Success() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.REJECTED);

        when(subscriptionService.rejectSubscription(1L)).thenReturn(subscription);

        ResponseEntity<SubscriptionResponse> response = controller.rejectSubscription(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PolicySubscription.SubscriptionStatus.REJECTED, response.getBody().getStatus());
        verify(subscriptionService).rejectSubscription(1L);
    }

    @Test
    void testAssignSurveyorForInspection_Success() {
        Inspection inspection = new Inspection();
        inspection.setInspectionId(1L);

        subscription.setPropertyInspection(inspection);
        subscription.setStatus(PolicySubscription.SubscriptionStatus.PENDING);

        when(subscriptionService.assignSurveyorForInspection(1L, 1L)).thenReturn(subscription);

        ResponseEntity<SubscriptionResponse> response = controller.assignSurveyorForInspection(1L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PolicySubscription.SubscriptionStatus.PENDING, response.getBody().getStatus());
        assertEquals(1L, response.getBody().getInspectionId());
        verify(subscriptionService).assignSurveyorForInspection(1L, 1L);
    }

    @Test
    void testGetSubscriptionById_Success() {
        when(subscriptionService.getSubscriptionById(1L)).thenReturn(subscription);

        ResponseEntity<SubscriptionResponse> response = controller.getSubscriptionById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getSubscriptionId());
        verify(subscriptionService).getSubscriptionById(1L);
    }

    @Test
    void testGetRenewalEligibleSubscriptions_Success() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);
        subscription.setRenewalEligible(true);
        subscription.setEndDate(LocalDate.now().plusDays(20));

        List<PolicySubscription> eligibleSubs = Arrays.asList(subscription);

        when(authentication.getName()).thenReturn("customer1");
        when(subscriptionService.getRenewalEligibleSubscriptions("customer1")).thenReturn(eligibleSubs);

        ResponseEntity<List<SubscriptionResponse>> response = controller.getRenewalEligibleSubscriptions(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertTrue(response.getBody().get(0).getRenewalEligible());
        verify(subscriptionService).getRenewalEligibleSubscriptions("customer1");
    }

    @Test
    void testRenewPolicy_Success() {
        subscription.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);
        subscription.setRenewalEligible(true);
        subscription.setEndDate(LocalDate.now().plusDays(20));

        PolicySubscription renewed = new PolicySubscription();
        renewed.setSubscriptionId(2L);
        renewed.setCustomer(customer);
        renewed.setProperty(property);
        renewed.setPolicy(policy);
        renewed.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);
        renewed.setPreviousSubscriptionId(1L);
        renewed.setRenewalCount(1);
        renewed.setStartDate(subscription.getEndDate());
        renewed.setEndDate(subscription.getEndDate().plusMonths(12));

        when(authentication.getName()).thenReturn("customer1");
        when(subscriptionService.getSubscriptionById(1L)).thenReturn(subscription);
        when(subscriptionService.renewPolicy(1L)).thenReturn(renewed);

        ResponseEntity<SubscriptionResponse> response = controller.renewPolicy(authentication, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2L, response.getBody().getSubscriptionId());
        assertEquals(1L, response.getBody().getPreviousSubscriptionId());
        assertEquals(1, response.getBody().getRenewalCount());
        verify(subscriptionService).renewPolicy(1L);
    }

    @Test
    void testRenewPolicy_Forbidden_NotOwner() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("customer2");

        Customer otherCustomer = new Customer();
        otherCustomer.setCustomerId(2L);
        otherCustomer.setUser(otherUser);

        subscription.setCustomer(otherCustomer);

        when(authentication.getName()).thenReturn("customer1");
        when(subscriptionService.getSubscriptionById(1L)).thenReturn(subscription);

        ResponseEntity<SubscriptionResponse> response = controller.renewPolicy(authentication, 1L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(subscriptionService, never()).renewPolicy(anyLong());
    }

    @Test
    void testGetNCBBenefits_Success() {
        subscription.setClaimFreeYears(3);

        when(subscriptionService.getSubscriptionById(1L)).thenReturn(subscription);
        when(subscriptionService.getNCBBenefitsDescription(3)).thenReturn("You have earned 25% NCB discount for 3 claim-free year(s).");

        ResponseEntity<String> response = controller.getNCBBenefits(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("25%"));
        verify(subscriptionService).getNCBBenefitsDescription(3);
    }

    @Test
    void testCalculateNCBDiscount_Success() {
        when(subscriptionService.calculateNCBDiscount(2)).thenReturn(0.20);

        ResponseEntity<Double> response = controller.calculateNCBDiscount(2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0.20, response.getBody());
        verify(subscriptionService).calculateNCBDiscount(2);
    }

    @Test
    void testUpdateRenewalEligibility_Success() {
        doNothing().when(subscriptionService).updateRenewalEligibility();

        ResponseEntity<String> response = controller.updateRenewalEligibility();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Renewal eligibility updated successfully", response.getBody());
        verify(subscriptionService).updateRenewalEligibility();
    }

    @Test
    void testMapToResponse_WithAllFields() {
        subscription.setSubscriptionId(1L);
        subscription.setStatus(PolicySubscription.SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(12));
        subscription.setPremiumAmount(12000.0);
        subscription.setBasePremiumAmount(10000.0);
        subscription.setRiskScore(5.0);
        subscription.setRiskMultiplier(1.2);
        subscription.setRenewalEligible(true);
        subscription.setPreviousSubscriptionId(null);
        subscription.setRenewalCount(0);
        subscription.setClaimFreeYears(2);
        subscription.setNcbDiscount(0.20);

        Inspection inspection = new Inspection();
        inspection.setInspectionId(1L);
        subscription.setPropertyInspection(inspection);

        when(subscriptionService.getSubscriptionById(1L)).thenReturn(subscription);

        ResponseEntity<SubscriptionResponse> response = controller.getSubscriptionById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SubscriptionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(1L, body.getSubscriptionId());
        assertEquals(12000.0, body.getPremiumAmount());
        assertEquals(10000.0, body.getBasePremiumAmount());
        assertEquals(5.0, body.getRiskScore());
        assertEquals(1.2, body.getRiskMultiplier());
        assertEquals(1L, body.getInspectionId());
        assertTrue(body.getRenewalEligible());
        assertEquals(0, body.getRenewalCount());
        assertEquals(2, body.getClaimFreeYears());
        assertEquals(0.20, body.getNcbDiscount());
    }

    @Test
    void testMapToResponse_WithNullFields() {
        subscription.setSubscriptionId(1L);
        subscription.setStatus(PolicySubscription.SubscriptionStatus.REQUESTED);
        subscription.setBasePremiumAmount(10000.0);

        when(subscriptionService.getSubscriptionById(1L)).thenReturn(subscription);

        ResponseEntity<SubscriptionResponse> response = controller.getSubscriptionById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SubscriptionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(1L, body.getSubscriptionId());
        assertNull(body.getPremiumAmount());
        assertNull(body.getRiskScore());
        assertNull(body.getRiskMultiplier());
        assertNull(body.getInspectionId());
        assertFalse(body.getRenewalEligible());
        assertEquals(0, body.getRenewalCount());
        assertEquals(0, body.getClaimFreeYears());
        assertEquals(0.0, body.getNcbDiscount());
    }
}
