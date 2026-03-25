package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.AssignUnderwriterRequest;
import org.hartford.fireinsurance.dto.SiuInvestigatorRegistrationRequest;
import org.hartford.fireinsurance.dto.SiuInvestigatorResponse;
import org.hartford.fireinsurance.dto.SurveyorRegistrationRequest;
import org.hartford.fireinsurance.dto.SurveyorResponse;
import org.hartford.fireinsurance.dto.SubscriptionResponse;
import org.hartford.fireinsurance.dto.UnderwriterRegistrationRequest;
import org.hartford.fireinsurance.dto.UnderwriterResponse;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.SiuInvestigator;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.model.Underwriter;
import org.hartford.fireinsurance.service.ClaimService;
import org.hartford.fireinsurance.service.PolicySubscriptionService;
import org.hartford.fireinsurance.service.SiuInvestigatorService;
import org.hartford.fireinsurance.service.SurveyorService;
import org.hartford.fireinsurance.service.UnderwriterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UnderwriterService underwriterService;
    private final SurveyorService surveyorService;
    private final SiuInvestigatorService siuInvestigatorService;
    private final PolicySubscriptionService subscriptionService;
    private final ClaimService claimService;

    public AdminController(UnderwriterService underwriterService,
                           SurveyorService surveyorService,
                           SiuInvestigatorService siuInvestigatorService,
                           PolicySubscriptionService subscriptionService,
                           ClaimService claimService) {
        this.underwriterService = underwriterService;
        this.surveyorService = surveyorService;
        this.siuInvestigatorService = siuInvestigatorService;
        this.subscriptionService = subscriptionService;
        this.claimService = claimService;
    }

    @PostMapping("/underwriters")
    public ResponseEntity<UnderwriterResponse> createUnderwriter(@RequestBody UnderwriterRegistrationRequest request) {
        Underwriter created = underwriterService.createUnderwriter(request);
        return ResponseEntity.ok(toUnderwriterResponse(created));
    }

    @GetMapping("/underwriters")
    public ResponseEntity<List<UnderwriterResponse>> getUnderwriters() {
        List<UnderwriterResponse> response = underwriterService.getAllUnderwriters().stream()
                .map(this::toUnderwriterResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/surveyors")
    public ResponseEntity<SurveyorResponse> createSurveyor(@RequestBody SurveyorRegistrationRequest request) {
        surveyorService.registerSurveyor(request);
        Surveyor created = surveyorService.getSurveyorByUsername(request.getUsername());
        return ResponseEntity.ok(toSurveyorResponse(created));
    }

    @GetMapping("/surveyors")
    public ResponseEntity<List<SurveyorResponse>> getSurveyors() {
        List<SurveyorResponse> response = surveyorService.getAllSurveyors().stream()
                .map(this::toSurveyorResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    // SIU Investigator Management Endpoints
    @PostMapping("/siu-investigators")
    public ResponseEntity<SiuInvestigatorResponse> createSiuInvestigator(@RequestBody SiuInvestigatorRegistrationRequest request) {
        SiuInvestigator created = siuInvestigatorService.registerSiuInvestigator(request);
        return ResponseEntity.ok(new SiuInvestigatorResponse(created));
    }

    @GetMapping("/siu-investigators")
    public ResponseEntity<List<SiuInvestigatorResponse>> getSiuInvestigators() {
        List<SiuInvestigatorResponse> response = siuInvestigatorService.getAllActiveInvestigators().stream()
                .map(SiuInvestigatorResponse::new)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/siu-investigators/{investigatorId}")
    public ResponseEntity<SiuInvestigatorResponse> getSiuInvestigator(@PathVariable Long investigatorId) {
        SiuInvestigator investigator = siuInvestigatorService.getInvestigatorById(investigatorId);
        return ResponseEntity.ok(new SiuInvestigatorResponse(investigator));
    }

    @PutMapping("/siu-investigators/{investigatorId}/deactivate")
    public ResponseEntity<String> deactivateSiuInvestigator(@PathVariable Long investigatorId) {
        siuInvestigatorService.deactivateInvestigator(investigatorId);
        return ResponseEntity.ok("SIU Investigator deactivated successfully");
    }

    @PutMapping("/siu-investigators/{investigatorId}/activate")
    public ResponseEntity<String> activateSiuInvestigator(@PathVariable Long investigatorId) {
        siuInvestigatorService.activateInvestigator(investigatorId);
        return ResponseEntity.ok("SIU Investigator activated successfully");
    }

    @PostMapping("/assign-underwriter/subscription")
    public ResponseEntity<SubscriptionResponse> assignUnderwriterToSubscription(@RequestBody AssignUnderwriterRequest request) {
        PolicySubscription updated = subscriptionService.assignUnderwriter(request.getTargetId(), request.getUnderwriterId());
        return ResponseEntity.ok(new SubscriptionResponse(
                updated.getSubscriptionId(),
                updated.getProperty() != null ? updated.getProperty().getPropertyId() : null,
                updated.getPolicy() != null ? updated.getPolicy().getPolicyName() : null,
                updated.getStartDate(),
                updated.getEndDate(),
                updated.getStatus(),
                updated.getPremiumAmount(),
                updated.getBasePremiumAmount(),
                updated.getRiskScore(),
                updated.getRiskMultiplier(),
                updated.getPropertyInspection() != null ? updated.getPropertyInspection().getInspectionId() : null,
                updated.getUnderwriter() != null ? updated.getUnderwriter().getUnderwriterId() : null,
                updated.getRequestedCoverage()
        ));
    }

    @PostMapping("/assign-underwriter/claim")
    public ResponseEntity<String> assignUnderwriterToClaim(@RequestBody AssignUnderwriterRequest request) {
        Claim updated = claimService.assignUnderwriter(request.getTargetId(), request.getUnderwriterId());
        return ResponseEntity.ok("Underwriter assigned to claim " + updated.getClaimId());
    }

    private UnderwriterResponse toUnderwriterResponse(Underwriter u) {
        return new UnderwriterResponse(
                u.getUnderwriterId(),
                u.getUser().getUsername(),
                u.getUser().getEmail(),
                u.getUser().getPhoneNumber(),
                u.getDepartment(),
                u.getRegion(),
                u.getExperienceYears(),
                u.getActive(),
                u.getCreatedAt()
        );
    }

    private SurveyorResponse toSurveyorResponse(Surveyor surveyor) {
        return new SurveyorResponse(
                surveyor.getSurveyorId(),
                surveyor.getUser().getUsername(),
                surveyor.getUser().getEmail(),
                surveyor.getUser().getPhoneNumber(),
                surveyor.getLicenseNumber(),
                surveyor.getExperienceYears(),
                surveyor.getAssignedRegion()
        );
    }
}

