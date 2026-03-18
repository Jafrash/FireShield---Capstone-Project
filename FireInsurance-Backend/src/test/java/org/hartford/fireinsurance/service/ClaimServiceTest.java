package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.CreateClaimRequest;
import org.hartford.fireinsurance.model.*;
import org.hartford.fireinsurance.repository.ClaimInspectionRepository;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.hartford.fireinsurance.repository.PolicySubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PolicySubscriptionRepository subscriptionRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private ClaimInspectionRepository claimInspectionRepository;

    @InjectMocks
    private ClaimService claimService;

    private Claim claim;
    private PolicySubscription subscription;
    private Customer customer;
    private User user;
    private CreateClaimRequest createRequest;
    private Policy policy;
    private Property property;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("customer1");
        user.setEmail("customer1@example.com");

        customer = new Customer();
        customer.setCustomerId(1L);
        customer.setUser(user);

        policy = new Policy();
        policy.setPolicyId(1L);
        policy.setMaxCoverageAmount(100000.0);
        policy.setDeductible(5000.0);

        property = new Property();
        property.setPropertyId(1L);
        property.setPropertyAge(10);

        subscription = new PolicySubscription();
        subscription.setSubscriptionId(1L);
        subscription.setCustomer(customer);
        subscription.setPolicy(policy);
        subscription.setProperty(property);

        claim = new Claim();
        claim.setClaimId(1L);
        claim.setSubscription(subscription);
        claim.setDescription("Fire damage");
        claim.setClaimAmount(50000.0);
        claim.setStatus(Claim.ClaimStatus.SUBMITTED);
        claim.setCreatedAt(LocalDateTime.now());
        claim.setIncidentDate(LocalDate.now());

        createRequest = new CreateClaimRequest();
        createRequest.setSubscriptionId(1L);
        createRequest.setDescription("Fire damage");
        createRequest.setClaimAmount(50000.0);
        createRequest.setIncidentDate(LocalDate.now());
    }

    @Test
    @DisplayName("createClaim - Success")
    void createClaim_Success() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));
        when(claimRepository.save(any(Claim.class))).thenReturn(claim);

        Claim result = claimService.createClaim("customer1", createRequest);

        assertNotNull(result);
        assertEquals("Fire damage", result.getDescription());
        assertEquals(50000.0, result.getClaimAmount());
        assertEquals(Claim.ClaimStatus.SUBMITTED, result.getStatus());
        verify(subscriptionRepository, times(1)).findById(1L);
        verify(claimRepository, times(1)).save(any(Claim.class));
    }

    @Test
    @DisplayName("createClaim - Subscription Not Found")
    void createClaim_SubscriptionNotFound() {
        when(subscriptionRepository.findById(999L)).thenReturn(Optional.empty());
        createRequest.setSubscriptionId(999L);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> claimService.createClaim("customer1", createRequest)
        );

        assertEquals("Subscription not found with ID: 999", exception.getMessage());
        verify(subscriptionRepository, times(1)).findById(999L);
        verify(claimRepository, never()).save(any(Claim.class));
    }

    @Test
    @DisplayName("createClaim - Unauthorized Subscription")
    void createClaim_UnauthorizedSubscription() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> claimService.createClaim("wronguser", createRequest)
        );

        assertEquals("Unauthorized: Subscription does not belong to this customer", exception.getMessage());
        verify(subscriptionRepository, times(1)).findById(1L);
        verify(claimRepository, never()).save(any(Claim.class));
    }

    @Test
    @DisplayName("getClaimsByUsername - Success")
    void getClaimsByUsername_Success() {
        when(customerService.getCustomerByUsername("customer1")).thenReturn(customer);
        when(claimRepository.findByCustomer(customer)).thenReturn(Arrays.asList(claim));

        List<Claim> result = claimService.getClaimsByUsername("customer1");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Fire damage", result.get(0).getDescription());
        verify(customerService, times(1)).getCustomerByUsername("customer1");
        verify(claimRepository, times(1)).findByCustomer(customer);
    }

    @Test
    @DisplayName("getAllClaims - Success")
    void getAllClaims_Success() {
        when(claimRepository.findAll()).thenReturn(Arrays.asList(claim));

        List<Claim> result = claimService.getAllClaims();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(claimRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("updateClaimStatus - Success")
    void updateClaimStatus_Success() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenReturn(claim);

        Claim result = claimService.updateClaimStatus(1L, Claim.ClaimStatus.UNDER_REVIEW);

        assertNotNull(result);
        assertEquals(Claim.ClaimStatus.UNDER_REVIEW, result.getStatus());
        verify(claimRepository, times(1)).findById(1L);
        verify(claimRepository, times(1)).save(any(Claim.class));
    }

    @Test
    @DisplayName("updateClaimStatus - Not Found")
    void updateClaimStatus_NotFound() {
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> claimService.updateClaimStatus(999L, Claim.ClaimStatus.UNDER_REVIEW)
        );

        assertEquals("Claim not found with ID: 999", exception.getMessage());
        verify(claimRepository, times(1)).findById(999L);
        verify(claimRepository, never()).save(any(Claim.class));
    }

    @Test
    @DisplayName("getClaimById - Success")
    void getClaimById_Success() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));

        Claim result = claimService.getClaimById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getClaimId());
        assertEquals("Fire damage", result.getDescription());
        verify(claimRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getClaimById - Not Found")
    void getClaimById_NotFound() {
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> claimService.getClaimById(999L)
        );

        assertEquals("Claim not found with ID: 999", exception.getMessage());
        verify(claimRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("approveClaim - Success")
    void approveClaim_Success() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenReturn(claim);
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        Claim result = claimService.approveClaim(1L);

        assertNotNull(result);
        assertEquals(Claim.ClaimStatus.APPROVED, result.getStatus());
        verify(claimRepository, times(1)).findById(1L);
        verify(claimRepository, times(1)).save(any(Claim.class));
        verify(subscriptionRepository, times(1)).save(any(PolicySubscription.class));
    }

    @Test
    @DisplayName("approveClaim - Not Found")
    void approveClaim_NotFound() {
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> claimService.approveClaim(999L)
        );

        assertEquals("Claim not found", exception.getMessage());
        verify(claimRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("rejectClaim - Success")
    void rejectClaim_Success() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(Claim.class))).thenReturn(claim);

        Claim result = claimService.rejectClaim(1L);

        assertNotNull(result);
        assertEquals(Claim.ClaimStatus.REJECTED, result.getStatus());
        verify(claimRepository, times(1)).findById(1L);
        verify(claimRepository, times(1)).save(any(Claim.class));
    }

    @Test
    @DisplayName("rejectClaim - Not Found")
    void rejectClaim_NotFound() {
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> claimService.rejectClaim(999L)
        );

        assertEquals("Claim not found", exception.getMessage());
        verify(claimRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("getEstimatedLoss - From Claim Amount")
    void getEstimatedLoss_FromClaimAmount() {
        Double result = claimService.getEstimatedLoss(claim);

        assertNotNull(result);
        assertEquals(50000.0, result);
    }

    @Test
    @DisplayName("getDepreciationRate - Property Age 10 Years")
    void getDepreciationRate_Age10() {
        double result = claimService.getDepreciationRate(claim);

        assertEquals(0.10, result);
    }
}
