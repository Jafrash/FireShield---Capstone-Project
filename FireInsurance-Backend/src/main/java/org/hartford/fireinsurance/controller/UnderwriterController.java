package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.AssignSurveyorClaimInspectionRequest;
import org.hartford.fireinsurance.dto.AssignSurveyorPropertyInspectionRequest;
import org.hartford.fireinsurance.dto.ClaimResponse;
import org.hartford.fireinsurance.dto.DocumentResponse;
import org.hartford.fireinsurance.dto.SubscriptionResponse;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.Document;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.service.ClaimService;
import org.hartford.fireinsurance.service.UnderwriterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/underwriter")
@PreAuthorize("hasRole('UNDERWRITER')")
public class UnderwriterController {

    private final UnderwriterService underwriterService;
    private final ClaimService claimService;

    public UnderwriterController(UnderwriterService underwriterService, ClaimService claimService) {
        this.underwriterService = underwriterService;
        this.claimService = claimService;
    }

    @GetMapping("/subscriptions/assigned")
    public ResponseEntity<List<SubscriptionResponse>> getAssignedSubscriptions(Authentication authentication) {
        List<SubscriptionResponse> response = underwriterService.getAssignedPolicySubscriptions(authentication.getName())
                .stream().map(this::toSubscriptionResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/claims/assigned")
    public ResponseEntity<List<ClaimResponse>> getAssignedClaims(Authentication authentication) {
        List<ClaimResponse> response = underwriterService.getAssignedClaims(authentication.getName())
                .stream().map(this::toClaimResponse).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assign-surveyor/property")
    public ResponseEntity<SubscriptionResponse> assignSurveyorForProperty(
            Authentication authentication,
            @RequestBody AssignSurveyorPropertyInspectionRequest request) {
        PolicySubscription updated = underwriterService.assignSurveyorForPropertyInspection(authentication.getName(), request);
        return ResponseEntity.ok(toSubscriptionResponse(updated));
    }

    @PostMapping("/assign-surveyor/claim")
    public ResponseEntity<ClaimResponse> assignSurveyorForClaim(
            Authentication authentication,
            @RequestBody AssignSurveyorClaimInspectionRequest request) {
        Claim updated = underwriterService.assignSurveyorForClaimInspection(authentication.getName(), request);
        return ResponseEntity.ok(toClaimResponse(updated));
    }

    @PostMapping("/subscriptions/{id}/approve")
    public ResponseEntity<SubscriptionResponse> approveSubscription(Authentication authentication, @PathVariable Long id) {
        PolicySubscription updated = underwriterService.approvePolicySubscription(authentication.getName(), id);
        return ResponseEntity.ok(toSubscriptionResponse(updated));
    }

    @PostMapping("/subscriptions/{id}/reject")
    public ResponseEntity<SubscriptionResponse> rejectSubscription(Authentication authentication, @PathVariable Long id) {
        PolicySubscription updated = underwriterService.rejectPolicySubscription(authentication.getName(), id);
        return ResponseEntity.ok(toSubscriptionResponse(updated));
    }

    @PostMapping("/claims/{id}/approve")
    public ResponseEntity<ClaimResponse> approveClaim(Authentication authentication, @PathVariable Long id) {
        Claim updated = underwriterService.approveClaim(authentication.getName(), id);
        return ResponseEntity.ok(toClaimResponse(updated));
    }

    @PostMapping("/claims/{id}/reject")
    public ResponseEntity<ClaimResponse> rejectClaim(Authentication authentication, @PathVariable Long id) {
        Claim updated = underwriterService.rejectClaim(authentication.getName(), id);
        return ResponseEntity.ok(toClaimResponse(updated));
    }

    @GetMapping("/subscriptions/{id}/documents")
    public ResponseEntity<List<DocumentResponse>> getSubscriptionDocuments(Authentication authentication, @PathVariable Long id) {
        List<DocumentResponse> response = underwriterService.getSubscriptionDocuments(authentication.getName(), id)
                .stream().map(this::toDocumentResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/claims/{id}/documents")
    public ResponseEntity<List<DocumentResponse>> getClaimDocuments(Authentication authentication, @PathVariable Long id) {
        List<DocumentResponse> response = underwriterService.getClaimDocuments(authentication.getName(), id)
                .stream().map(this::toDocumentResponse).toList();
        return ResponseEntity.ok(response);
    }

    private SubscriptionResponse toSubscriptionResponse(PolicySubscription sub) {
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
        response.setRenewalEligible(sub.getRenewalEligible() != null ? sub.getRenewalEligible() : false);
        response.setPreviousSubscriptionId(sub.getPreviousSubscriptionId());
        response.setRenewalCount(sub.getRenewalCount() != null ? sub.getRenewalCount() : 0);
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

    private ClaimResponse toClaimResponse(Claim claim) {
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

    private DocumentResponse toDocumentResponse(Document document) {
        return new DocumentResponse(
                document.getDocumentId(),
                document.getFileName(),
                document.getDocumentType(),
                document.getDocumentStage(),
                document.getFileSize(),
                document.getContentType(),
                document.getUploadDate(),
                document.getUploadedBy()
        );
    }
}
