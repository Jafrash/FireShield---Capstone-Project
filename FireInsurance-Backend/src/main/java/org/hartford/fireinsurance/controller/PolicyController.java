package org.hartford.fireinsurance.controller;
import org.hartford.fireinsurance.dto.CreatePolicyRequest;
import org.hartford.fireinsurance.dto.PolicyResponse;
import org.hartford.fireinsurance.dto.PremiumCalculationFactors;
import org.hartford.fireinsurance.dto.UpdatePolicyRequest;
import org.hartford.fireinsurance.model.Policy;
import org.hartford.fireinsurance.service.PolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/policies")
public class PolicyController {
    private final PolicyService policyService;
    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> createPolicy(@RequestBody CreatePolicyRequest request) {
        Policy policy = policyService.createPolicy(request);
        return ResponseEntity.ok(mapToResponse(policy));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> updatePolicy(@PathVariable Long id, @RequestBody UpdatePolicyRequest request) {
        Policy updated = policyService.updatePolicy(id, request);
        return ResponseEntity.ok(mapToResponse(updated));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return ResponseEntity.ok("Policy deleted successfully");
    }
    @GetMapping
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        List<PolicyResponse> response = policyService.getAllPolicies().stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(response);
    }

    // Alias endpoint for backward compatibility - /me returns all policies for customers
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<List<PolicyResponse>> getMyPolicies() {
        List<PolicyResponse> response = policyService.getAllPolicies().stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<PolicyResponse> getPolicyById(@PathVariable Long id) {
        Policy policy = policyService.getPolicyById(id);
        return ResponseEntity.ok(mapToResponse(policy));
    }
    private PolicyResponse mapToResponse(Policy policy) {
        // Calculate default premium factors (computed on-the-fly, not stored in DB)
        PremiumCalculationFactors factors = new PremiumCalculationFactors(
            1.2,  // riskMultiplier - default medium risk
            1.1,  // locationFactor - default urban area
            1.0,  // constructionFactor - default standard construction
            1.0   // ageFactor - default no age penalty
        );
        
        PolicyResponse response = new PolicyResponse(
            policy.getPolicyId(),
            policy.getPolicyName(),
            policy.getCoverageDetails(),
            policy.getBasePremium(),
            policy.getMaxCoverageAmount(),
            policy.getDurationMonths(),
            factors
        );
        
        return response;
    }
}
