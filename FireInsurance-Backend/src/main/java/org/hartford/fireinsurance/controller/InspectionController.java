package org.hartford.fireinsurance.controller;
import org.hartford.fireinsurance.dto.InspectionRequest;
import org.hartford.fireinsurance.dto.InspectionDocumentSummary;
import org.hartford.fireinsurance.dto.InspectionResponse;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Document;
import org.hartford.fireinsurance.model.Inspection;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.service.DocumentService;
import org.hartford.fireinsurance.service.InspectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/inspections")
public class InspectionController {
    private final InspectionService inspectionService;
    private final DocumentService documentService;
    public InspectionController(InspectionService inspectionService, DocumentService documentService) {
        this.inspectionService = inspectionService;
        this.documentService = documentService;
    }
    @PostMapping("/assign/{propertyId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER')")
    public ResponseEntity<InspectionResponse> assignSurveyor(@PathVariable Long propertyId, @RequestParam Long surveyorId) {
        Inspection inspection = inspectionService.assignSurveyor(propertyId, surveyorId);
        return ResponseEntity.ok(mapToResponse(inspection));
    }
    @PutMapping("/{inspectionId}/submit")
    @PreAuthorize("hasRole('SURVEYOR')")
    public ResponseEntity<InspectionResponse> submitInspection(Authentication authentication, @PathVariable Long inspectionId, @RequestBody InspectionRequest request) {
        String username = authentication.getName();
        Inspection inspection = inspectionService.submitInspection(inspectionId, username, request);
        return ResponseEntity.ok(mapToResponse(inspection));
    }
    @GetMapping("/me")
    @PreAuthorize("hasRole('SURVEYOR')")
    public ResponseEntity<List<InspectionResponse>> getMyInspections(Authentication authentication) {
        String username = authentication.getName();
        List<InspectionResponse> response = inspectionService.getBySurveyor(username).stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(response);
    }
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER')")
    public ResponseEntity<?> getAll(
            Authentication authentication) {
        try {
            List<InspectionResponse> response = inspectionService.getAll()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching inspections: " + e.getMessage());
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
    public ResponseEntity<InspectionResponse> getById(@PathVariable Long id) {
        Inspection inspection = inspectionService.getById(id);
        return ResponseEntity.ok(mapToResponse(inspection));
    }
    private InspectionResponse mapToResponse(Inspection inspection) {
        Property property = inspection.getProperty();
        Customer customer = property != null ? property.getCustomer() : null;
        InspectionResponse response = new InspectionResponse(
                inspection.getInspectionId(),
            property != null ? property.getPropertyId() : null,
                inspection.getSurveyor() != null && inspection.getSurveyor().getUser() != null ? inspection.getSurveyor().getUser().getUsername() : "Unknown",
                inspection.getInspectionDate(),
                inspection.getAssessedRiskScore(),
                inspection.getStatus());
        response.setCustomerName(customer != null && customer.getUser() != null ? customer.getUser().getUsername() : null);
        response.setCustomerEmail(customer != null && customer.getUser() != null ? customer.getUser().getEmail() : null);
        response.setCustomerPhone(customer != null && customer.getUser() != null ? customer.getUser().getPhoneNumber() : null);
        response.setPropertyAddress(property != null ? property.getAddress() : null);
        response.setPropertyType(property != null ? property.getPropertyType() : null);

        if (property != null && customer != null && customer.getUser() != null) {
            String customerUsername = customer.getUser().getUsername();
            List<InspectionDocumentSummary> customerDocs = documentService.getDocumentsByProperty(property.getPropertyId())
                .stream()
                .filter(doc -> doc.getCustomer() != null)
                .filter(doc -> customerUsername.equals(doc.getUploadedBy()))
                .map(this::mapToDocumentSummary)
                .toList();
            response.setCustomerDocuments(customerDocs);
        }

        response.setFireSafetyAvailable(inspection.getFireSafetyAvailable());
        response.setSprinklerSystem(inspection.getSprinklerSystem());
        response.setFireExtinguishers(inspection.getFireExtinguishers());
        response.setDistanceFromFireStation(inspection.getDistanceFromFireStation());
        response.setConstructionRisk(inspection.getConstructionRisk());
        response.setHazardRisk(inspection.getHazardRisk());
        response.setRecommendedCoverage(inspection.getRecommendedCoverage());
        response.setRecommendedPremium(inspection.getRecommendedPremium());

        // Map Expanded COPE fields
        response.setConstructionType(inspection.getConstructionType());
        response.setRoofType(inspection.getRoofType());
        response.setOccupancyType(inspection.getOccupancyType());
        response.setElectricalAuditStatus(inspection.getElectricalAuditStatus());
        response.setHazardousMaterialsPresent(inspection.getHazardousMaterialsPresent());
        response.setAdjacentBuildingDistance(inspection.getAdjacentBuildingDistance());
        response.setInternalProtectionNotes(inspection.getInternalProtectionNotes());
        
        // Link to requested sum insured from related subscription
        if (inspection.getSubscription() != null) {
            response.setRequestedSumInsured(inspection.getSubscription().getRequestedCoverage());
        }
        
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
