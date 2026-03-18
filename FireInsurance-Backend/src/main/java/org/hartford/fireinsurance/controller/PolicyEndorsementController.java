package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.CreateEndorsementRequest;
import org.hartford.fireinsurance.dto.EndorsementResponse;
import org.hartford.fireinsurance.model.PolicyEndorsement;
import org.hartford.fireinsurance.service.PolicyEndorsementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/endorsements")
public class PolicyEndorsementController {

    private final PolicyEndorsementService endorsementService;

    public PolicyEndorsementController(PolicyEndorsementService endorsementService) {
        this.endorsementService = endorsementService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<EndorsementResponse> create(Authentication authentication,
                                                      @RequestBody CreateEndorsementRequest request) {
        PolicyEndorsement endorsement = endorsementService.create(authentication.getName(), request);
        return ResponseEntity.ok(map(endorsement));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER')")
    public ResponseEntity<EndorsementResponse> approve(Authentication authentication, @PathVariable Long id) {
        PolicyEndorsement endorsement = endorsementService.review(id, true, authentication.getName());
        return ResponseEntity.ok(map(endorsement));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER')")
    public ResponseEntity<EndorsementResponse> reject(Authentication authentication, @PathVariable Long id) {
        PolicyEndorsement endorsement = endorsementService.review(id, false, authentication.getName());
        return ResponseEntity.ok(map(endorsement));
    }

    @GetMapping("/subscription/{subscriptionId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<EndorsementResponse>> getForSubscription(Authentication authentication,
                                                                        @PathVariable Long subscriptionId) {
        return ResponseEntity.ok(endorsementService.getBySubscription(subscriptionId, authentication.getName())
                .stream().map(this::map).toList());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EndorsementResponse>> getAll() {
        return ResponseEntity.ok(endorsementService.getAll().stream().map(this::map).toList());
    }

    private EndorsementResponse map(PolicyEndorsement e) {
        EndorsementResponse r = new EndorsementResponse();
        r.setEndorsementId(e.getEndorsementId());
        r.setSubscriptionId(e.getSubscription() != null ? e.getSubscription().getSubscriptionId() : null);
        r.setChangeType(e.getChangeType());
        r.setRequestedCoverage(e.getRequestedCoverage());
        r.setNewOccupancyType(e.getNewOccupancyType());
        r.setNewHazardousGoods(e.getNewHazardousGoods());
        r.setReason(e.getReason());
        r.setStatus(e.getStatus());
        r.setRequestedBy(e.getRequestedBy());
        r.setReviewedBy(e.getReviewedBy());
        r.setCreatedAt(e.getCreatedAt());
        r.setReviewedAt(e.getReviewedAt());
        return r;
    }
}
