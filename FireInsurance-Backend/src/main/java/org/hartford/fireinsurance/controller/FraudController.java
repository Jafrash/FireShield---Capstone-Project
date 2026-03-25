package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.AddInvestigationNoteRequest;
import org.hartford.fireinsurance.dto.AddTimelineEntryRequest;
import org.hartford.fireinsurance.dto.BlacklistRequest;
import org.hartford.fireinsurance.dto.BlacklistResponse;
import org.hartford.fireinsurance.dto.CreateInvestigationCaseRequest;
import org.hartford.fireinsurance.dto.EvidenceUpdateRequest;
import org.hartford.fireinsurance.dto.FraudAnalysisResponse;
import org.hartford.fireinsurance.dto.InvestigationNotesRequest;
import org.hartford.fireinsurance.dto.SiuAssignmentRequest;
import org.hartford.fireinsurance.dto.SiuAssignmentResponse;
import org.hartford.fireinsurance.dto.SiuCaseResponse;
import org.hartford.fireinsurance.dto.SiuInvestigatorResponse;
import org.hartford.fireinsurance.dto.UpdateCaseStatusRequest;
import org.hartford.fireinsurance.model.Blacklist.BlacklistType;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.Evidence;
import org.hartford.fireinsurance.model.InvestigationActivity;
import org.hartford.fireinsurance.model.InvestigationCase;
import org.hartford.fireinsurance.model.SiuInvestigator;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.hartford.fireinsurance.repository.InvestigationCaseRepository;
import org.hartford.fireinsurance.service.BlacklistService;
import org.hartford.fireinsurance.service.ClaimService;
import org.hartford.fireinsurance.service.EvidenceService;
import org.hartford.fireinsurance.service.FraudDetectionService;
import org.hartford.fireinsurance.service.InvestigationActivityService;
import org.hartford.fireinsurance.service.InvestigationCaseService;
import org.hartford.fireinsurance.service.SiuInvestigatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST Controller for fraud detection and blacklist management.
 */
@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    private static final Logger log = LoggerFactory.getLogger(FraudController.class);

    private final FraudDetectionService fraudDetectionService;
    private final BlacklistService blacklistService;
    private final ClaimService claimService;
    private final SiuInvestigatorService siuInvestigatorService;
    private final InvestigationCaseService investigationCaseService;
    private final EvidenceService evidenceService;
    private final InvestigationActivityService investigationActivityService;
    private final ClaimRepository claimRepository;
    private final InvestigationCaseRepository investigationCaseRepository;

    public FraudController(FraudDetectionService fraudDetectionService,
                          BlacklistService blacklistService,
                          ClaimService claimService,
                          SiuInvestigatorService siuInvestigatorService,
                          InvestigationCaseService investigationCaseService,
                          EvidenceService evidenceService,
                          InvestigationActivityService investigationActivityService,
                          ClaimRepository claimRepository,
                          InvestigationCaseRepository investigationCaseRepository) {
        this.fraudDetectionService = fraudDetectionService;
        this.blacklistService = blacklistService;
        this.claimService = claimService;
        this.siuInvestigatorService = siuInvestigatorService;
        this.investigationCaseService = investigationCaseService;
        this.evidenceService = evidenceService;
        this.investigationActivityService = investigationActivityService;
        this.claimRepository = claimRepository;
        this.investigationCaseRepository = investigationCaseRepository;
    }

    // ==================== BLACKLIST ENDPOINTS ====================

    /**
     * Add a new entry to the blacklist.
     */
    @PostMapping("/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BlacklistResponse> addToBlacklist(
            @RequestBody BlacklistRequest request,
            Authentication authentication) {
        log.info("Adding to blacklist: type={}, value={}", request.getType(), request.getValue());
        String username = authentication.getName();
        BlacklistResponse response = blacklistService.addToBlacklist(request, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Debug endpoint - Get raw blacklist count.
     */
    @GetMapping("/blacklist/debug/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getBlacklistDebugCount() {
        try {
            long count = blacklistService.getAllBlacklist().size();
            log.info("Blacklist debug count: {}", count);
            return ResponseEntity.ok("Blacklist entries count: " + count);
        } catch (Exception e) {
            log.error("Error in blacklist debug count: ", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get all active blacklist entries.
     */
    @GetMapping("/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BlacklistResponse>> getAllBlacklist() {
        try {
            log.info("Getting all blacklist entries...");
            List<BlacklistResponse> entries = blacklistService.getAllBlacklist();
            log.info("Successfully retrieved {} blacklist entries", entries.size());
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            log.error("Error retrieving blacklist entries: ", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Get blacklist entries by type.
     */
    @GetMapping("/blacklist/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BlacklistResponse>> getBlacklistByType(@PathVariable BlacklistType type) {
        List<BlacklistResponse> entries = blacklistService.getBlacklistByType(type);
        return ResponseEntity.ok(entries);
    }

    /**
     * Remove (soft-delete) a blacklist entry.
     */
    @DeleteMapping("/blacklist/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeFromBlacklist(@PathVariable Long id) {
        log.info("Removing blacklist entry ID: {}", id);
        blacklistService.removeFromBlacklist(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== FRAUD ANALYSIS ENDPOINTS ====================

    /**
     * Get fraud analysis for a specific claim.
     */
    @GetMapping("/analysis/{claimId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SIU_INVESTIGATOR')")
    @Transactional(readOnly = true)
    public ResponseEntity<FraudAnalysisResponse> getFraudAnalysis(@PathVariable Long claimId) {
        log.info("Fetching fraud analysis for Claim ID: {}", claimId);
        Claim claim = claimService.getClaimById(claimId);
        FraudAnalysisResponse analysis = fraudDetectionService.calculateFraudScore(claim);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Recalculate fraud score for an existing claim.
     */
    @PostMapping("/analysis/{claimId}/recalculate")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<FraudAnalysisResponse> recalculateFraudScore(@PathVariable Long claimId) {
        log.info("Recalculating fraud score for Claim ID: {}", claimId);
        Claim claim = claimService.getClaimById(claimId);
        // applyFraudAnalysis calculates AND saves, so we can calculate it again one last time to return DTO
        // Actually applyFraudAnalysis should return the analysis or we can just call calculateFraudScore
        FraudAnalysisResponse analysis = fraudDetectionService.calculateFraudScore(claim);
        
        // Update claim with results
        claim.setFraudScore(analysis.getFraudScore());
        claim.setRiskLevel(analysis.getRiskLevel());
        claim.setFraudStatus(analysis.getFraudStatus());
        claim.setFraudAnalysisTimestamp(java.time.LocalDateTime.now());
        claimService.updateClaimStatus(claim.getClaimId(), claim.getStatus()); // Force save through service if needed
        
        return ResponseEntity.ok(analysis);
    }

    // ==================== PATTERN DETECTION ENDPOINTS ====================

    /**
     * Check if a customer has frequent claims (pattern detection utility).
     */
    @GetMapping("/patterns/frequent-claims/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SIU_INVESTIGATOR')")
    public ResponseEntity<Boolean> checkFrequentClaims(@PathVariable Long customerId) {
        boolean isFrequent = fraudDetectionService.isFrequentClaims(customerId);
        return ResponseEntity.ok(isFrequent);
    }

    /**
     * Check if an address is used by multiple customers (pattern detection utility).
     */
    @GetMapping("/patterns/duplicate-address")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SIU_INVESTIGATOR')")
    public ResponseEntity<Boolean> checkDuplicateAddress(
            @RequestParam String address,
            @RequestParam Long excludeCustomerId) {
        boolean isDuplicate = fraudDetectionService.isDuplicateAddress(address, excludeCustomerId);
        return ResponseEntity.ok(isDuplicate);
    }

    // ==================== SIU CASE MANAGEMENT ====================

    /**
     * Get all available SIU investigators for assignment.
     */
    @GetMapping("/siu/investigators")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<List<SiuInvestigatorResponse>> getAvailableInvestigators() {
        List<SiuInvestigatorResponse> investigators = siuInvestigatorService.getAllActiveInvestigators()
                .stream()
                .map(SiuInvestigatorResponse::new)
                .toList();
        return ResponseEntity.ok(investigators);
    }

    /**
     * Get all claims under SIU investigation.
     */
    @GetMapping("/siu/cases")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<List<SiuCaseResponse>> getSiuCases() {
        List<Claim> siuCases = claimService.getSiuCases();
        List<SiuCaseResponse> response = siuCases.stream()
                .map(SiuCaseResponse::new)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get claims assigned to a specific SIU investigator.
     */
    @GetMapping("/siu/investigator/{investigatorId}/cases")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SIU_INVESTIGATOR')")
    public ResponseEntity<List<SiuCaseResponse>> getClaimsByInvestigator(@PathVariable Long investigatorId) {
        List<Claim> claims = claimService.getClaimsBySiuInvestigator(investigatorId);
        List<SiuCaseResponse> response = claims.stream()
                .map(SiuCaseResponse::new)
                .toList();
        log.info("Retrieved {} claims for investigator ID: {}", response.size(), investigatorId);
        return ResponseEntity.ok(response);
    }

    /**
     * Assign claim to SIU investigator.
     */
    @PostMapping("/siu/assign/{claimId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<SiuAssignmentResponse> assignClaimToSiu(
            @PathVariable Long claimId,
            @RequestBody SiuAssignmentRequest request) {
        try {
            log.info("Assigning claim {} to SIU investigator {}", claimId, request.getSiuInvestigatorId());

            // Validate inputs
            if (request.getSiuInvestigatorId() == null) {
                log.error("SIU investigator ID is null in assignment request");
                return ResponseEntity.badRequest()
                    .body(new SiuAssignmentResponse(claimId, "SIU investigator ID is required"));
            }

            Claim updatedClaim = claimService.assignToSiu(claimId, request.getSiuInvestigatorId(), request.getInitialNotes());

            log.info("Successfully assigned claim {} to SIU investigator {}", claimId, request.getSiuInvestigatorId());

            // Return simple DTO to avoid JSON circular references
            SiuAssignmentResponse response = new SiuAssignmentResponse(updatedClaim);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error assigning claim {} to SIU investigator {}: {}", claimId, request.getSiuInvestigatorId(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(new SiuAssignmentResponse(claimId, "Assignment failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error assigning claim {} to SIU: {}", claimId, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(new SiuAssignmentResponse(claimId, "Internal server error during assignment"));
        }
    }

    /**
     * Update investigation notes for a claim.
     */
    @PostMapping("/siu/notes/{claimId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SIU_INVESTIGATOR')")
    public ResponseEntity<Claim> updateInvestigationNotes(
            @PathVariable Long claimId,
            @RequestBody InvestigationNotesRequest request) {
        log.info("Updating investigation notes for claim {}", claimId);
        Claim updatedClaim = claimService.updateInvestigationNotes(claimId, request.getNotes(), request.getNewStatus());
        return ResponseEntity.ok(updatedClaim);
    }

    // ==================== ENHANCED SIU INVESTIGATION CASE MANAGEMENT ====================

    /**
     * Create a new investigation case for a claim.
     */
    @PostMapping("/siu/cases/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InvestigationCase> createInvestigationCase(
            @RequestBody CreateInvestigationCaseRequest request,
            Authentication authentication) {
        log.info("Creating investigation case for claim ID: {} with investigator ID: {}",
                request.getClaimId(), request.getInvestigatorId());

        String createdBy = authentication.getName();
        InvestigationCase investigationCase = investigationCaseService.createInvestigationCase(
                request.getClaimId(),
                request.getInvestigatorId(),
                request.getInitialNotes(),
                createdBy
        );
        return ResponseEntity.ok(investigationCase);
    }

    /**
     * Get all investigation cases assigned to a specific investigator.
     */
    @GetMapping("/siu/investigator/{investigatorId}/assigned-cases")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<List<InvestigationCase>> getAssignedCases(@PathVariable Long investigatorId) {
        log.info("Fetching assigned cases for investigator ID: {}", investigatorId);
        List<InvestigationCase> cases = investigationCaseService.getCasesByInvestigator(investigatorId);
        return ResponseEntity.ok(cases);
    }

    /**
     * Get complete details of a specific investigation case.
     */
    @GetMapping("/siu/case/{investigationId}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SIU_INVESTIGATOR')")
    public ResponseEntity<InvestigationCase> getCaseDetails(@PathVariable Long investigationId) {
        log.info("Fetching details for investigation case ID: {}", investigationId);
        InvestigationCase investigationCase = investigationCaseService.getCaseById(investigationId);
        return ResponseEntity.ok(investigationCase);
    }

    /**
     * Update the status of an investigation case.
     */
    @PostMapping("/siu/case/{investigationId}/update-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<InvestigationCase> updateCaseStatus(
            @PathVariable Long investigationId,
            @RequestBody UpdateCaseStatusRequest request,
            Authentication authentication) {
        log.info("Updating status for investigation case ID: {} to: {}", investigationId, request.getStatus());

        String updatedBy = authentication.getName();
        InvestigationCase updatedCase = investigationCaseService.updateCaseStatus(
                investigationId,
                request.getStatus(),
                updatedBy
        );
        return ResponseEntity.ok(updatedCase);
    }

    /**
     * Add investigation note to a case.
     */
    @PostMapping("/siu/case/{investigationId}/add-note")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<InvestigationCase> addInvestigationNote(
            @PathVariable Long investigationId,
            @RequestBody AddInvestigationNoteRequest request,
            Authentication authentication) {
        log.info("Adding investigation note to case ID: {}", investigationId);

        String addedBy = authentication.getName();
        InvestigationCase updatedCase = investigationCaseService.addInvestigationNote(
                investigationId,
                request.getNote(),
                addedBy
        );
        return ResponseEntity.ok(updatedCase);
    }

    /**
     * Get all investigation cases for SIU dashboard overview.
     */
    @GetMapping("/siu/cases/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<List<InvestigationCase>> getAllSiuCases() {
        log.info("Fetching all SIU investigation cases");
        List<InvestigationCase> cases = investigationCaseService.getAllSiuCases();
        return ResponseEntity.ok(cases);
    }

    /**
     * Get case statistics for a specific investigator.
     */
    @GetMapping("/siu/investigator/{investigatorId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<InvestigationCaseService.CaseStatistics> getInvestigatorStatistics(@PathVariable Long investigatorId) {
        log.info("Fetching case statistics for investigator ID: {}", investigatorId);
        InvestigationCaseService.CaseStatistics statistics = investigationCaseService.getCaseStatistics(investigatorId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Debug endpoint to check SIU assignment data
     */
    @GetMapping("/siu/debug/investigator/{investigatorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<Map<String, Object>> debugInvestigatorData(@PathVariable Long investigatorId) {
        log.info("🔍 Debug: Checking data for investigator ID: {}", investigatorId);

        Map<String, Object> debugInfo = new HashMap<>();

        // Check if investigator exists
        try {
            SiuInvestigator investigator = siuInvestigatorService.getInvestigatorById(investigatorId);
            debugInfo.put("investigator", Map.of(
                "id", investigator.getInvestigatorId(),
                "username", investigator.getUsername(),
                "badge", investigator.getBadgeNumber(),
                "active", investigator.getActive()
            ));
        } catch (Exception e) {
            debugInfo.put("investigatorError", e.getMessage());
        }

        // Check assigned claims
        List<Claim> assignedClaims = claimService.getClaimsBySiuInvestigator(investigatorId);
        debugInfo.put("assignedClaimsCount", assignedClaims.size());
        debugInfo.put("assignedClaims", assignedClaims.stream()
            .map(claim -> Map.of(
                "claimId", claim.getClaimId(),
                "status", claim.getStatus().toString(),
                "fraudStatus", claim.getFraudStatus().toString(),
                "siuInvestigatorId", claim.getSiuInvestigatorId() != null ? claim.getSiuInvestigatorId() : "null"
            ))
            .toList());

        // Check all SIU cases in system
        List<Claim> allSiuCases = claimService.getSiuCases();
        debugInfo.put("totalSiuCasesInSystem", allSiuCases.size());

        // Check investigation cases
        try {
            List<InvestigationCase> investigationCases = investigationCaseService.getCasesByInvestigator(investigatorId);
            debugInfo.put("investigationCasesCount", investigationCases.size());
        } catch (Exception e) {
            debugInfo.put("investigationCasesError", e.getMessage());
        }

        log.info("📊 Debug results for investigator {}: {} assigned claims, {} total SIU cases",
                investigatorId, assignedClaims.size(), allSiuCases.size());

        return ResponseEntity.ok(debugInfo);
    }

    // ==================== EVIDENCE MANAGEMENT ENDPOINTS ====================

    /**
     * Upload evidence file for an investigation case.
     */
    @PostMapping("/siu/case/{investigationCaseId}/evidence/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<Evidence> uploadEvidence(
            @PathVariable Long investigationCaseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) Set<String> tags,
            @RequestParam(value = "isCritical", defaultValue = "false") Boolean isCritical,
            Authentication authentication) {
        try {
            log.info("Uploading evidence for investigation case ID: {}, file: {}",
                     investigationCaseId, file.getOriginalFilename());

            String uploadedBy = authentication.getName();
            Evidence evidence = evidenceService.uploadEvidence(investigationCaseId, file, description,
                                                             tags, isCritical, uploadedBy);
            return ResponseEntity.ok(evidence);

        } catch (Exception e) {
            log.error("Error uploading evidence for case ID: " + investigationCaseId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all evidence files for an investigation case.
     */
    @GetMapping("/siu/case/{investigationCaseId}/evidence")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR', 'UNDERWRITER')")
    public ResponseEntity<List<Evidence>> getCaseEvidence(@PathVariable Long investigationCaseId) {
        log.info("Fetching evidence for investigation case ID: {}", investigationCaseId);
        List<Evidence> evidence = evidenceService.getEvidenceByCase(investigationCaseId);
        return ResponseEntity.ok(evidence);
    }

    /**
     * Get specific evidence file details.
     */
    @GetMapping("/siu/evidence/{evidenceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR', 'UNDERWRITER')")
    public ResponseEntity<Evidence> getEvidenceDetails(@PathVariable Long evidenceId) {
        log.info("Fetching evidence details for ID: {}", evidenceId);
        Evidence evidence = evidenceService.getEvidenceById(evidenceId);
        return ResponseEntity.ok(evidence);
    }

    /**
     * Delete evidence file.
     */
    @DeleteMapping("/siu/evidence/{evidenceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<Void> deleteEvidence(@PathVariable Long evidenceId, Authentication authentication) {
        log.info("Deleting evidence ID: {}", evidenceId);
        String deletedBy = authentication.getName();
        evidenceService.deleteEvidence(evidenceId, deletedBy);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update evidence metadata.
     */
    @PutMapping("/siu/evidence/{evidenceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<Evidence> updateEvidenceMetadata(
            @PathVariable Long evidenceId,
            @RequestBody EvidenceUpdateRequest request,
            Authentication authentication) {
        log.info("Updating evidence metadata for ID: {}", evidenceId);

        String updatedBy = authentication.getName();
        Evidence evidence = evidenceService.updateEvidenceMetadata(evidenceId,
                request.getDescription(), request.getTags(), request.getIsCritical(), updatedBy);
        return ResponseEntity.ok(evidence);
    }

    /**
     * Search evidence by criteria.
     */
    @GetMapping("/siu/case/{investigationCaseId}/evidence/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR', 'UNDERWRITER')")
    public ResponseEntity<List<Evidence>> searchEvidence(
            @PathVariable Long investigationCaseId,
            @RequestParam(value = "fileType", required = false) Evidence.EvidenceType fileType,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @RequestParam(value = "isCritical", required = false) Boolean isCritical,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {

        log.info("Searching evidence for investigation case ID: {} with criteria", investigationCaseId);
        List<Evidence> evidence = evidenceService.searchEvidence(investigationCaseId, fileType,
                searchTerm, isCritical, uploadedBy);
        return ResponseEntity.ok(evidence);
    }

    /**
     * Get evidence statistics for an investigation case.
     */
    @GetMapping("/siu/case/{investigationCaseId}/evidence/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR', 'UNDERWRITER')")
    public ResponseEntity<EvidenceService.EvidenceStatistics> getEvidenceStatistics(@PathVariable Long investigationCaseId) {
        log.info("Fetching evidence statistics for investigation case ID: {}", investigationCaseId);
        EvidenceService.EvidenceStatistics statistics = evidenceService.getEvidenceStatistics(investigationCaseId);
        return ResponseEntity.ok(statistics);
    }

    // ==================== INVESTIGATION TIMELINE ENDPOINTS ====================

    /**
     * Add custom timeline entry to investigation case.
     */
    @PostMapping("/siu/case/{investigationCaseId}/timeline-entry")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public ResponseEntity<InvestigationActivity> addTimelineEntry(
            @PathVariable Long investigationCaseId,
            @RequestBody AddTimelineEntryRequest request,
            Authentication authentication) {
        log.info("Adding timeline entry to investigation case ID: {}", investigationCaseId);

        String performedBy = authentication.getName();
        InvestigationActivity activity = investigationActivityService.addActivity(
                investigationCaseId, request.getActivityType(), request.getDescription(),
                performedBy, request.getAdditionalDetails());
        return ResponseEntity.ok(activity);
    }

    /**
     * Get complete investigation timeline for a case.
     */
    @GetMapping("/siu/case/{investigationCaseId}/timeline")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR', 'UNDERWRITER')")
    public ResponseEntity<List<InvestigationActivity>> getInvestigationTimeline(@PathVariable Long investigationCaseId) {
        log.info("Fetching investigation timeline for case ID: {}", investigationCaseId);
        List<InvestigationActivity> timeline = investigationActivityService.getTimelineByCase(investigationCaseId);
        return ResponseEntity.ok(timeline);
    }

    /**
     * Get recent activities for an investigation case.
     */
    @GetMapping("/siu/case/{investigationCaseId}/timeline/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR', 'UNDERWRITER')")
    public ResponseEntity<List<InvestigationActivity>> getRecentActivities(
            @PathVariable Long investigationCaseId,
            @RequestParam(value = "days", defaultValue = "7") int days) {
        log.info("Fetching recent activities for case ID: {} (last {} days)", investigationCaseId, days);
        List<InvestigationActivity> activities = investigationActivityService.getRecentActivities(investigationCaseId, days);
        return ResponseEntity.ok(activities);
    }

    /**
     * Search investigation activities.
     */
    @GetMapping("/siu/case/{investigationCaseId}/timeline/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR', 'UNDERWRITER')")
    public ResponseEntity<List<InvestigationActivity>> searchActivities(
            @PathVariable Long investigationCaseId,
            @RequestParam("searchTerm") String searchTerm) {
        log.info("Searching activities for case ID: {} with term: {}", investigationCaseId, searchTerm);
        List<InvestigationActivity> activities = investigationActivityService.searchActivities(investigationCaseId, searchTerm);
        return ResponseEntity.ok(activities);
    }

    /**
     * Get activity statistics for an investigation case.
     */
    @GetMapping("/siu/case/{investigationCaseId}/timeline/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR', 'UNDERWRITER')")
    public ResponseEntity<InvestigationActivityService.ActivityStatistics> getActivityStatistics(@PathVariable Long investigationCaseId) {
        log.info("Fetching activity statistics for investigation case ID: {}", investigationCaseId);
        InvestigationActivityService.ActivityStatistics statistics = investigationActivityService.getActivityStatistics(investigationCaseId);
        return ResponseEntity.ok(statistics);
    }

    // ==================== ADMINISTRATIVE REPAIR ENDPOINTS ====================

    /**
     * Sync claim assignments with investigation cases (recovery mechanism)
     */
    @PostMapping("/siu/sync-assignments")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> syncClaimAssignments() {
        log.info("🔄 Starting sync of claim assignments with investigation cases");

        try {
            // Get all claims that are assigned to SIU but may not have investigation cases
            List<Claim> siuClaims = claimRepository.findByFraudStatus(Claim.FraudStatus.SIU_INVESTIGATION);

            Map<String, Object> result = new HashMap<>();
            int syncedCases = 0;
            List<String> errors = new ArrayList<>();

            for (Claim claim : siuClaims) {
                if (claim.getSiuInvestigatorId() != null) {
                    try {
                        // Check if investigation case already exists
                        Optional<InvestigationCase> existingCase = investigationCaseRepository.findByClaim(claim);

                        if (!existingCase.isPresent()) {
                            // Create missing investigation case
                            investigationCaseService.createInvestigationCase(
                                claim.getClaimId(),
                                claim.getSiuInvestigatorId(),
                                "Synced assignment - Case created during sync process",
                                "system-sync"
                            );
                            syncedCases++;
                            log.info("✅ Created investigation case for claim {} assigned to investigator {}",
                                    claim.getClaimId(), claim.getSiuInvestigatorId());
                        }
                    } catch (Exception e) {
                        String error = String.format("Failed to sync claim %d: %s", claim.getClaimId(), e.getMessage());
                        errors.add(error);
                        log.error("❌ {}", error);
                    }
                }
            }

            result.put("totalSiuClaims", siuClaims.size());
            result.put("syncedCases", syncedCases);
            result.put("errors", errors);
            result.put("success", errors.isEmpty());

            log.info("🎯 Sync completed: {} SIU claims checked, {} cases created, {} errors",
                    siuClaims.size(), syncedCases, errors.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Sync process failed: {}", e.getMessage());
            return ResponseEntity.status(500)
                .body(Map.of("success", false, "error", "Sync process failed: " + e.getMessage()));
        }
    }
}
