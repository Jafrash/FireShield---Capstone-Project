package org.hartford.fireinsurance.controller;
import org.hartford.fireinsurance.dto.SubscribeRequest;
import org.hartford.fireinsurance.dto.PremiumBreakdownResponse;
import org.hartford.fireinsurance.dto.SubscriptionResponse;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.service.PolicySubscriptionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class PolicySubscriptionController {
    private final PolicySubscriptionService subscriptionService;
    
    public PolicySubscriptionController(PolicySubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }
    
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SubscriptionResponse> subscribe(Authentication authentication, @RequestBody SubscribeRequest request) {
        String username = authentication.getName();
        System.err.println("[SUB-API] Received subscription request for user: " + username + " on property ID: " + request.getPropertyId());
        PolicySubscription subscription = subscriptionService.subscribe(username, request);
        System.err.println("[SUB-API] Subscription successfully created, mapping to response...");
        return ResponseEntity.ok(mapToResponse(subscription));
    }
    
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(Authentication authentication) {
        String username = authentication.getName();
        List<SubscriptionResponse> response = subscriptionService.getSubscriptionsByUsername(username).stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions() {
        List<SubscriptionResponse> response = subscriptionService.getAllSubscriptions().stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(@PathVariable Long id) {
        PolicySubscription updated = subscriptionService.cancelSubscription(id);
        return ResponseEntity.ok(mapToResponse(updated));
    }
    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('UNDERWRITER')")
    public ResponseEntity<SubscriptionResponse> approveSubscription(@PathVariable Long id) {
        PolicySubscription updated = subscriptionService.approveSubscription(id);
        return ResponseEntity.ok(mapToResponse(updated));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('UNDERWRITER')")
    public ResponseEntity<SubscriptionResponse> rejectSubscription(@PathVariable Long id) {
        PolicySubscription updated = subscriptionService.rejectSubscription(id);
        return ResponseEntity.ok(mapToResponse(updated));
    }
    
    @PutMapping("/{id}/assign-surveyor/{surveyorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionResponse> assignSurveyorForInspection(@PathVariable Long id, @PathVariable Long surveyorId) {
        PolicySubscription updated = subscriptionService.assignSurveyorForInspection(id, surveyorId);
        return ResponseEntity.ok(mapToResponse(updated));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','UNDERWRITER')")
    public ResponseEntity<SubscriptionResponse> getSubscriptionById(@PathVariable Long id) {
        PolicySubscription subscription = subscriptionService.getSubscriptionById(id);
        return ResponseEntity.ok(mapToResponse(subscription));
    }
    
    // ========== RENEWAL WORKFLOW ENDPOINTS ==========
    
    /**
     * Get renewal eligible subscriptions for logged-in customer
     */
    @GetMapping("/me/renewal-eligible")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<SubscriptionResponse>> getRenewalEligibleSubscriptions(Authentication authentication) {
        String username = authentication.getName();
        List<SubscriptionResponse> response = subscriptionService.getRenewalEligibleSubscriptions(username)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Renew an existing policy subscription
     */
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SubscriptionResponse> renewPolicy(Authentication authentication, @PathVariable Long id) {
        String username = authentication.getName();
        
        // Verify the subscription belongs to the customer
        PolicySubscription existingSubscription = subscriptionService.getSubscriptionById(id);
        if (!existingSubscription.getCustomer().getUser().getUsername().equals(username)) {
            return ResponseEntity.status(403).build(); // Forbidden
        }
        
        PolicySubscription renewedSubscription = subscriptionService.renewPolicy(id);
        return ResponseEntity.ok(mapToResponse(renewedSubscription));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<SubscriptionResponse> acceptPayment(Authentication authentication, @PathVariable Long id) {
        String username = authentication.getName();
        PolicySubscription existingSubscription = subscriptionService.getSubscriptionById(id);
        if (!existingSubscription.getCustomer().getUser().getUsername().equals(username)) {
            return ResponseEntity.status(403).build();
        }
        PolicySubscription updated = subscriptionService.acceptPayment(id);
        return ResponseEntity.ok(mapToResponse(updated));
    }

    @GetMapping(value = "/{id}/documents/cover-note", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<byte[]> downloadCoverNote(Authentication authentication, @PathVariable Long id) {
        String username = authentication.getName();
        PolicySubscription existingSubscription = subscriptionService.getSubscriptionById(id);
        if (!existingSubscription.getCustomer().getUser().getUsername().equals(username)) {
            return ResponseEntity.status(403).build();
        }

        byte[] content = subscriptionService.getCoverNoteBytes(id);
        String fileName = existingSubscription.getCoverNoteFileName() != null && !existingSubscription.getCoverNoteFileName().isBlank()
                ? existingSubscription.getCoverNoteFileName()
                : "CoverNote-SUB" + id + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @GetMapping(value = "/{id}/documents/policy", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<byte[]> downloadPolicyDocument(Authentication authentication, @PathVariable Long id) {
        String username = authentication.getName();
        PolicySubscription existingSubscription = subscriptionService.getSubscriptionById(id);
        if (!existingSubscription.getCustomer().getUser().getUsername().equals(username)) {
            return ResponseEntity.status(403).build();
        }

        byte[] content = subscriptionService.getPolicyDocumentBytes(id);
        String fileName = existingSubscription.getPolicyDocumentFileName() != null && !existingSubscription.getPolicyDocumentFileName().isBlank()
                ? existingSubscription.getPolicyDocumentFileName()
                : "PolicyDocument-SUB" + id + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @GetMapping("/{id}/premium-breakdown")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PremiumBreakdownResponse> getPremiumBreakdown(Authentication authentication, @PathVariable Long id) {
        String username = authentication.getName();
        PolicySubscription existingSubscription = subscriptionService.getSubscriptionById(id);
        if (!existingSubscription.getCustomer().getUser().getUsername().equals(username)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(subscriptionService.getPremiumBreakdown(id));
    }
    
    /**
     * Get NCB benefits description for a subscription
     */
    @GetMapping("/{id}/ncb-benefits")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<String> getNCBBenefits(@PathVariable Long id) {
        PolicySubscription subscription = subscriptionService.getSubscriptionById(id);
        String benefits = subscriptionService.getNCBBenefitsDescription(subscription.getClaimFreeYears());
        return ResponseEntity.ok(benefits);
    }
    
    /**
     * Calculate NCB discount for given claim-free years
     */
    @GetMapping("/ncb-calculator/{years}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<Double> calculateNCBDiscount(@PathVariable Integer years) {
        Double discount = subscriptionService.calculateNCBDiscount(years);
        return ResponseEntity.ok(discount);
    }
    
    /**
     * Admin endpoint to update renewal eligibility for all active subscriptions
     * This should be called by a scheduled task daily
     */
    @PostMapping("/admin/update-renewal-eligibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateRenewalEligibility() {
        subscriptionService.updateRenewalEligibility();
        return ResponseEntity.ok("Renewal eligibility updated successfully");
    }
    
    private SubscriptionResponse mapToResponse(PolicySubscription sub) {
        SubscriptionResponse response = new SubscriptionResponse(
            sub.getSubscriptionId(), 
            sub.getProperty() != null ? sub.getProperty().getPropertyId() : null, 
            sub.getPolicy() != null ? sub.getPolicy().getPolicyName() : null, 
            sub.getStartDate(), 
            sub.getEndDate(), 
            sub.getStatus(), 
            sub.getPremiumAmount(),
            sub.getBasePremiumAmount(),
            sub.getRiskScore(),
            sub.getRiskMultiplier(),
            sub.getPropertyInspection() != null ? sub.getPropertyInspection().getInspectionId() : null,
            sub.getUnderwriter() != null ? sub.getUnderwriter().getUnderwriterId() : null,
            sub.getRequestedCoverage()
        );
        
        // Add renewal and NCB fields
        response.setRenewalEligible(sub.getRenewalEligible() != null ? sub.getRenewalEligible() : false);
        response.setPreviousSubscriptionId(sub.getPreviousSubscriptionId());
        response.setRenewalCount(sub.getRenewalCount() != null ? sub.getRenewalCount() : 0);
        response.setDaysRemaining(sub.getDaysRemaining());
        response.setClaimFreeYears(sub.getClaimFreeYears() != null ? sub.getClaimFreeYears() : 0);
        response.setNcbDiscount(sub.getNcbDiscount() != null ? sub.getNcbDiscount() : 0.0);

        response.setPaymentReceived(sub.getPaymentReceived() != null ? sub.getPaymentReceived() : false);
        response.setCoverNoteFileName(sub.getCoverNoteFileName());
        response.setPolicyDocumentFileName(sub.getPolicyDocumentFileName());

        response.setConstructionType(sub.getConstructionType());
        response.setRoofType(sub.getRoofType());
        response.setNumberOfFloors(sub.getNumberOfFloors());
        response.setOccupancyType(sub.getOccupancyType());
        response.setManufacturingProcess(sub.getManufacturingProcess());
        response.setHazardousGoods(sub.getHazardousGoods());
        response.setPreviousLossHistory(sub.getPreviousLossHistory());
        response.setInsuranceDeclinedBefore(sub.getInsuranceDeclinedBefore());
        response.setPropertyValue(sub.getPropertyValue());
        
        return response;
    }
}
