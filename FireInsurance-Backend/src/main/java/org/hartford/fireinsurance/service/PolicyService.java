package org.hartford.fireinsurance.service;


import org.hartford.fireinsurance.dto.CreatePolicyRequest;
import org.hartford.fireinsurance.dto.UpdatePolicyRequest;
import org.hartford.fireinsurance.exception.ResourceNotFoundException;
import org.hartford.fireinsurance.model.Policy;
import org.hartford.fireinsurance.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PolicyService {

    private final PolicyRepository policyRepository;

    public PolicyService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    /**
     * Create a new policy (ADMIN only)
     */
    public Policy createPolicy(CreatePolicyRequest request) {
        Policy policy = new Policy();
        policy.setPolicyName(request.getPolicyName());
        policy.setCoverageDetails(request.getCoverageDetails());
        policy.setBasePremium(request.getBasePremium());
        policy.setMaxCoverageAmount(request.getMaxCoverageAmount());
        policy.setDurationMonths(request.getDurationMonths());

        return policyRepository.save(policy);
    }

    /**
     * Update an existing policy (ADMIN only)
     */
    public Policy updatePolicy(Long id, UpdatePolicyRequest request) {
        Policy policy = getPolicyById(id);

        // Update fields (null-safe)
        if (request.getPolicyName() != null) {
            policy.setPolicyName(request.getPolicyName());
        }
        if (request.getCoverageDetails() != null) {
            policy.setCoverageDetails(request.getCoverageDetails());
        }
        if (request.getBasePremium() != null) {
            policy.setBasePremium(request.getBasePremium());
        }
        if (request.getMaxCoverageAmount() != null) {
            policy.setMaxCoverageAmount(request.getMaxCoverageAmount());
        }
        if (request.getDurationMonths() != null) {
            policy.setDurationMonths(request.getDurationMonths());
        }

        return policyRepository.save(policy);
    }

    /**
     * Delete a policy (ADMIN only)
     */
    public void deletePolicy(Long id) {
        if (!policyRepository.existsById(id)) {
            throw new RuntimeException("Policy not found with ID: " + id);
        }
        policyRepository.deleteById(id);
    }

    /**
     * Get all policies (CUSTOMER & ADMIN)
     */
    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }

    /**
     * Get policy by ID (CUSTOMER & ADMIN)
     */
    public Policy getPolicyById(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with ID: " + id));
    }

    // ========== OLD METHODS (for backward compatibility) ==========

    public Policy createPolicy(Policy policy) {
        return policyRepository.save(policy);
    }

    public List<Policy> getPolicies() {
        return policyRepository.findAll();
    }
}

