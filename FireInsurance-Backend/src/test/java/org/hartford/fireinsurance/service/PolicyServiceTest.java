package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.CreatePolicyRequest;
import org.hartford.fireinsurance.dto.UpdatePolicyRequest;
import org.hartford.fireinsurance.model.Policy;
import org.hartford.fireinsurance.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyService policyService;

    private Policy policy;
    private CreatePolicyRequest createRequest;
    private UpdatePolicyRequest updateRequest;

    @BeforeEach
    void setUp() {
        policy = new Policy();
        policy.setPolicyId(1L);
        policy.setPolicyName("Basic Fire Insurance");
        policy.setCoverageDetails("Covers fire damage up to policy limit");
        policy.setBasePremium(1000.0);
        policy.setMaxCoverageAmount(100000.0);
        policy.setDurationMonths(12);

        createRequest = new CreatePolicyRequest();
        createRequest.setPolicyName("Basic Fire Insurance");
        createRequest.setCoverageDetails("Covers fire damage up to policy limit");
        createRequest.setBasePremium(1000.0);
        createRequest.setMaxCoverageAmount(100000.0);
        createRequest.setDurationMonths(12);

        updateRequest = new UpdatePolicyRequest();
        updateRequest.setPolicyName("Updated Fire Insurance");
        updateRequest.setBasePremium(1200.0);
    }

    @Test
    @DisplayName("createPolicy - Success")
    void createPolicy_Success() {
        when(policyRepository.save(any(Policy.class))).thenReturn(policy);

        Policy result = policyService.createPolicy(createRequest);

        assertNotNull(result);
        assertEquals("Basic Fire Insurance", result.getPolicyName());
        assertEquals("Covers fire damage up to policy limit", result.getCoverageDetails());
        assertEquals(1000.0, result.getBasePremium());
        assertEquals(100000.0, result.getMaxCoverageAmount());
        assertEquals(12, result.getDurationMonths());

        verify(policyRepository, times(1)).save(any(Policy.class));
    }

    @Test
    @DisplayName("updatePolicy - Success")
    void updatePolicy_Success() {
        Policy updatedPolicy = new Policy();
        updatedPolicy.setPolicyId(1L);
        updatedPolicy.setPolicyName("Updated Fire Insurance");
        updatedPolicy.setCoverageDetails("Covers fire damage up to policy limit");
        updatedPolicy.setBasePremium(1200.0);
        updatedPolicy.setMaxCoverageAmount(100000.0);
        updatedPolicy.setDurationMonths(12);

        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenReturn(updatedPolicy);

        Policy result = policyService.updatePolicy(1L, updateRequest);

        assertNotNull(result);
        assertEquals("Updated Fire Insurance", result.getPolicyName());
        assertEquals(1200.0, result.getBasePremium());

        verify(policyRepository, times(1)).findById(1L);
        verify(policyRepository, times(1)).save(any(Policy.class));
    }

    @Test
    @DisplayName("updatePolicy - Partial Update")
    void updatePolicy_PartialUpdate() {
        UpdatePolicyRequest partialRequest = new UpdatePolicyRequest();
        partialRequest.setPolicyName("Partially Updated");

        Policy partiallyUpdated = new Policy();
        partiallyUpdated.setPolicyId(1L);
        partiallyUpdated.setPolicyName("Partially Updated");
        partiallyUpdated.setCoverageDetails("Covers fire damage up to policy limit");
        partiallyUpdated.setBasePremium(1000.0);
        partiallyUpdated.setMaxCoverageAmount(100000.0);
        partiallyUpdated.setDurationMonths(12);

        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenReturn(partiallyUpdated);

        Policy result = policyService.updatePolicy(1L, partialRequest);

        assertNotNull(result);
        assertEquals("Partially Updated", result.getPolicyName());
        assertEquals(1000.0, result.getBasePremium());

        verify(policyRepository, times(1)).findById(1L);
        verify(policyRepository, times(1)).save(any(Policy.class));
    }

    @Test
    @DisplayName("updatePolicy - Not Found")
    void updatePolicy_NotFound() {
        when(policyRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            policyService.updatePolicy(999L, updateRequest);
        });

        assertEquals("Policy not found with ID: 999", exception.getMessage());
        verify(policyRepository, times(1)).findById(999L);
        verify(policyRepository, never()).save(any(Policy.class));
    }

    @Test
    @DisplayName("deletePolicy - Success")
    void deletePolicy_Success() {
        when(policyRepository.existsById(1L)).thenReturn(true);
        doNothing().when(policyRepository).deleteById(1L);

        assertDoesNotThrow(() -> policyService.deletePolicy(1L));

        verify(policyRepository, times(1)).existsById(1L);
        verify(policyRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deletePolicy - Not Found")
    void deletePolicy_NotFound() {
        when(policyRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            policyService.deletePolicy(999L);
        });

        assertEquals("Policy not found with ID: 999", exception.getMessage());
        verify(policyRepository, times(1)).existsById(999L);
        verify(policyRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("getAllPolicies - Success")
    void getAllPolicies_Success() {
        Policy policy2 = new Policy();
        policy2.setPolicyId(2L);
        policy2.setPolicyName("Premium Fire Insurance");
        policy2.setBasePremium(2000.0);

        List<Policy> policies = Arrays.asList(policy, policy2);
        when(policyRepository.findAll()).thenReturn(policies);

        List<Policy> result = policyService.getAllPolicies();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Basic Fire Insurance", result.get(0).getPolicyName());
        assertEquals("Premium Fire Insurance", result.get(1).getPolicyName());

        verify(policyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllPolicies - Empty List")
    void getAllPolicies_EmptyList() {
        when(policyRepository.findAll()).thenReturn(Arrays.asList());

        List<Policy> result = policyService.getAllPolicies();

        assertNotNull(result);
        assertEquals(0, result.size());

        verify(policyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getPolicyById - Success")
    void getPolicyById_Success() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));

        Policy result = policyService.getPolicyById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPolicyId());
        assertEquals("Basic Fire Insurance", result.getPolicyName());
        assertEquals(1000.0, result.getBasePremium());

        verify(policyRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getPolicyById - Not Found")
    void getPolicyById_NotFound() {
        when(policyRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            policyService.getPolicyById(999L);
        });

        assertEquals("Policy not found with ID: 999", exception.getMessage());
        verify(policyRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("createPolicy - Legacy Method")
    void createPolicy_LegacyMethod() {
        when(policyRepository.save(any(Policy.class))).thenReturn(policy);

        Policy result = policyService.createPolicy(policy);

        assertNotNull(result);
        assertEquals(1L, result.getPolicyId());
        assertEquals("Basic Fire Insurance", result.getPolicyName());

        verify(policyRepository, times(1)).save(policy);
    }
}
