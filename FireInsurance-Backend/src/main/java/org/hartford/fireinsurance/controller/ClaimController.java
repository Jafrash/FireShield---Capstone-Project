package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.ClaimResponse;
import org.hartford.fireinsurance.dto.CreateClaimRequest;
import org.hartford.fireinsurance.dto.UpdateClaimStatusRequest;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.service.ClaimService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> createClaim(Authentication authentication,
            @RequestBody CreateClaimRequest request) {
        String username = authentication.getName();
        Claim claim = claimService.createClaim(username, request);
        return ResponseEntity.ok(mapToResponse(claim));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ClaimResponse>> getMyClaims(Authentication authentication) {
        String username = authentication.getName();
        List<ClaimResponse> response = claimService.getClaimsByUsername(username).stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimResponse>> getAllClaims() {
        List<ClaimResponse> response = claimService.getAllClaims().stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SURVEYOR')")
    public ResponseEntity<ClaimResponse> updateClaimStatus(@PathVariable Long id,
            @RequestBody UpdateClaimStatusRequest request) {
        Claim updated = claimService.updateClaimStatus(id, request.getStatus());
        return ResponseEntity.ok(mapToResponse(updated));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('UNDERWRITER')")
    public ResponseEntity<ClaimResponse> approveClaim(@PathVariable Long id) {
        Claim approved = claimService.approveClaim(id);
        return ResponseEntity.ok(mapToResponse(approved));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('UNDERWRITER')")
    public ResponseEntity<ClaimResponse> rejectClaim(@PathVariable Long id) {
        Claim rejected = claimService.rejectClaim(id);
        return ResponseEntity.ok(mapToResponse(rejected));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','SURVEYOR','UNDERWRITER')")
    public ResponseEntity<ClaimResponse> getClaimById(@PathVariable Long id) {
        Claim claim = claimService.getClaimById(id);
        return ResponseEntity.ok(mapToResponse(claim));
    }

    private ClaimResponse mapToResponse(Claim claim) {
        return new ClaimResponse(
                claim.getClaimId(),
                claim.getSubscription() != null ? claim.getSubscription().getSubscriptionId() : null,
                claim.getDescription(),
                claim.getClaimAmount(),
                claim.getStatus(),
                claim.getCreatedAt(),
                claimService.getEstimatedLoss(claim),
                claimService.getCalculatedDeductible(claim),
                claimService.getCalculatedDepreciation(claim),
                claim.getSettlementAmount() != null ? claim.getSettlementAmount() : 0.0,
                claim.getUnderwriter() != null ? claim.getUnderwriter().getUnderwriterId() : null,
                claim.getRiskLevel() != null ? claim.getRiskLevel().name() : null,
                claim.getFraudScore()
        );
    }
}
