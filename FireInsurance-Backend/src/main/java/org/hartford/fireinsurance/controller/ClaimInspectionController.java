package org.hartford.fireinsurance.controller;
import org.hartford.fireinsurance.dto.ClaimInspectionRequest;
import org.hartford.fireinsurance.dto.ClaimInspectionResponse;
import org.hartford.fireinsurance.dto.InspectionDocumentSummary;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Document;
import org.hartford.fireinsurance.model.ClaimInspection;
import org.hartford.fireinsurance.service.DocumentService;
import org.hartford.fireinsurance.service.ClaimInspectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/claim-inspections")
public class ClaimInspectionController {
    private final ClaimInspectionService claimInspectionService;
    private final DocumentService documentService;
    public ClaimInspectionController(ClaimInspectionService claimInspectionService, DocumentService documentService) {
        this.claimInspectionService = claimInspectionService;
        this.documentService = documentService;
    }
    @PostMapping("/assign/{claimId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER')")
    public ResponseEntity<ClaimInspectionResponse> assignSurveyor(@PathVariable Long claimId, @RequestParam Long surveyorId) {
        ClaimInspection inspection = claimInspectionService.assignSurveyor(claimId, surveyorId);
        return ResponseEntity.ok(mapToResponse(inspection));
    }
    @PutMapping("/{inspectionId}/submit")
    @PreAuthorize("hasRole('SURVEYOR')")
    public ResponseEntity<ClaimInspectionResponse> submitInspection(Authentication authentication, @PathVariable Long inspectionId, @RequestBody ClaimInspectionRequest request) {
        String username = authentication.getName();
        ClaimInspection inspection = claimInspectionService.submitInspection(inspectionId, username, request);
        return ResponseEntity.ok(mapToResponse(inspection));
    }
    @GetMapping("/me")
    @PreAuthorize("hasRole('SURVEYOR')")
    public ResponseEntity<List<ClaimInspectionResponse>> getMyInspections(Authentication authentication) {
        String username = authentication.getName();
        List<ClaimInspectionResponse> response = claimInspectionService.getBySurveyor(username).stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(response);
    }
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER')")
    public ResponseEntity<?> getAll(
            Authentication authentication) {
        try {
            List<ClaimInspectionResponse> response = claimInspectionService.getAll()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching claim inspections: " + e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER')")
    public ResponseEntity<?> getAllWithFilter(
            Authentication authentication,
            @RequestParam(required = false) String status) {
        return getAll(authentication);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SURVEYOR','ADMIN', 'UNDERWRITER')")
    public ResponseEntity<ClaimInspectionResponse> getById(@PathVariable Long id) {
        ClaimInspection inspection = claimInspectionService.getById(id);
        return ResponseEntity.ok(mapToResponse(inspection));
    }
    @GetMapping("/claim/{claimId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SURVEYOR')")
    public ResponseEntity<ClaimInspectionResponse> getByClaimId(@PathVariable Long claimId) {
        ClaimInspection inspection = claimInspectionService.getByClaim(claimId);
        return ResponseEntity.ok(mapToResponse(inspection));
    }
    private ClaimInspectionResponse mapToResponse(ClaimInspection inspection) {
        Claim claim = inspection.getClaim();
        Customer customer = claim != null && claim.getSubscription() != null ? claim.getSubscription().getCustomer() : null;
        ClaimInspectionResponse response = new ClaimInspectionResponse(
                inspection.getClaimInspectionId(),
            claim != null ? claim.getClaimId() : null,
                inspection.getSurveyor() != null && inspection.getSurveyor().getUser() != null ? inspection.getSurveyor().getUser().getUsername() : "Unknown",
                inspection.getInspectionDate(),
                inspection.getEstimatedLoss(),
                inspection.getStatus());
        response.setCustomerName(customer != null && customer.getUser() != null ? customer.getUser().getUsername() : null);
        response.setCustomerEmail(customer != null && customer.getUser() != null ? customer.getUser().getEmail() : null);
        response.setCustomerPhone(customer != null && customer.getUser() != null ? customer.getUser().getPhoneNumber() : null);
        response.setRequestedClaimAmount(claim != null ? claim.getClaimAmount() : null);
        response.setClaimDescription(claim != null ? claim.getDescription() : null);

        if (claim != null && customer != null && customer.getUser() != null) {
            String customerUsername = customer.getUser().getUsername();
            List<InspectionDocumentSummary> customerDocs = documentService.getDocumentsByClaim(claim.getClaimId())
                .stream()
                .filter(doc -> doc.getCustomer() != null)
                .filter(doc -> customerUsername.equals(doc.getUploadedBy()))
                .map(this::mapToDocumentSummary)
                .toList();
            response.setCustomerDocuments(customerDocs);
        }

        response.setCauseOfFire(inspection.getCauseOfFire());
        response.setSalvageValue(inspection.getSalvageValue());
        response.setFireBrigadeExpenses(inspection.getFireBrigadeExpenses());
        response.setOtherInsuranceDetails(inspection.getOtherInsuranceDetails());
        response.setUnderInsuranceDetected(inspection.getUnderInsuranceDetected());
        response.setRecommendedSettlement(inspection.getRecommendedSettlement());
        return response;
    }

    private InspectionDocumentSummary mapToDocumentSummary(Document document) {
        return new InspectionDocumentSummary(
                document.getDocumentId(),
                document.getFileName(),
                document.getDocumentType(),
                document.getDocumentStage(),
                document.getUploadDate(),
                document.getUploadedBy());
    }
}
