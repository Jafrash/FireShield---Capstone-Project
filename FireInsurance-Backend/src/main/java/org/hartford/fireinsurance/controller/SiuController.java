package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.SiuClaimsResponse;
import org.hartford.fireinsurance.dto.SiuInvestigationActionRequest;
import org.hartford.fireinsurance.dto.SiuInvestigationActionResponse;
import org.hartford.fireinsurance.service.SiuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for SIU (Special Investigation Unit) operations.
 * Handles fraud investigation, suspicious claim management, and investigator dashboard.
 *
 * Access is restricted to users with SIU_INVESTIGATOR role.
 */
@RestController
@RequestMapping("/api/siu")
@PreAuthorize("hasRole('SIU_INVESTIGATOR')")
public class SiuController {

    private static final Logger log = LoggerFactory.getLogger(SiuController.class);

    private final SiuService siuService;

    public SiuController(SiuService siuService) {
        this.siuService = siuService;
    }

    /**
     * Get all claims assigned for SIU investigation.
     * Returns claims with fraud scores and investigation priorities.
     *
     * @param authentication Current user authentication context
     * @return SIU claims response with aggregated statistics
     */
    @GetMapping("/claims")
    public ResponseEntity<SiuClaimsResponse> getSiuClaims(Authentication authentication) {
        try {
            log.info("SIU investigator '{}' accessing claims dashboard", authentication.getName());

            SiuClaimsResponse response = siuService.getSiuClaims();

            log.info("Successfully retrieved {} SIU claims for investigator '{}'",
                    response.getTotalCount(), authentication.getName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving SIU claims for investigator '{}': {}",
                     authentication.getName(), e.getMessage(), e);

            // Return meaningful error response
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get specific claim details for investigation.
     *
     * @param claimId The claim ID to investigate
     * @param authentication Current user authentication context
     * @return Detailed claim information for investigation
     */
    @GetMapping("/claims/{claimId}")
    public ResponseEntity<?> getClaimDetails(@PathVariable String claimId, Authentication authentication) {
        try {
            log.info("SIU investigator '{}' accessing claim details for claim ID: {}",
                    authentication.getName(), claimId);

            Object claimDetails = siuService.getClaimDetailsForInvestigation(claimId);

            if (claimDetails == null) {
                log.warn("Claim not found or access denied for investigator '{}', claim '{}'",
                        authentication.getName(), claimId);
                return ResponseEntity.notFound().build();
            }

            log.info("Successfully retrieved claim details for investigator '{}', claim '{}'",
                    authentication.getName(), claimId);

            return ResponseEntity.ok(claimDetails);

        } catch (Exception e) {
            log.error("Error retrieving claim details for investigator '{}', claim '{}': {}",
                     authentication.getName(), claimId, e.getMessage(), e);

            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update claim investigation status.
     *
     * @param claimId The claim ID to update
     * @param body Request body containing new status
     * @param authentication Current user authentication context
     * @return Updated claim status
     */
    @PutMapping("/claims/{claimId}/status")
    public ResponseEntity<?> updateClaimStatus(@PathVariable String claimId,
                                             @RequestBody Object body,
                                             Authentication authentication) {
        try {
            log.info("SIU investigator '{}' updating status for claim ID: {}",
                    authentication.getName(), claimId);

            // TODO: Implement claim status update for SIU investigations
            // This would handle status changes like UNDER_INVESTIGATION,
            // SUSPICIOUS, CLEARED, FRAUD_CONFIRMED, etc.

            return ResponseEntity.ok().body("{\"message\": \"Status update endpoint - Implementation pending\"}");

        } catch (Exception e) {
            log.error("Error updating claim status for investigator '{}', claim '{}': {}",
                     authentication.getName(), claimId, e.getMessage(), e);

            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Submit investigation report for a claim.
     *
     * @param claimId The claim ID for the report
     * @param body Investigation report data
     * @param authentication Current user authentication context
     * @return Submission confirmation
     */
    @PostMapping("/claims/{claimId}/report")
    public ResponseEntity<?> submitInvestigationReport(@PathVariable String claimId,
                                                     @RequestBody Object body,
                                                     Authentication authentication) {
        try {
            log.info("SIU investigator '{}' submitting investigation report for claim ID: {}",
                    authentication.getName(), claimId);

            // TODO: Implement investigation report submission
            // This would handle detailed investigation findings,
            // evidence documentation, recommendations, etc.

            return ResponseEntity.ok().body("{\"message\": \"Report submission endpoint - Implementation pending\"}");

        } catch (Exception e) {
            log.error("Error submitting investigation report for investigator '{}', claim '{}': {}",
                     authentication.getName(), claimId, e.getMessage(), e);

            return ResponseEntity.internalServerError().build();
        }
    }

    // === SIU Investigation Actions ===

    /**
     * Start formal SIU investigation on a claim.
     * Changes claim status to indicate investigation has begun.
     *
     * @param request Investigation action request containing claim ID and notes
     * @param authentication Current user authentication context
     * @return Investigation action response with success status and updated claim status
     */
    @PostMapping("/start-investigation")
    public ResponseEntity<SiuInvestigationActionResponse> startInvestigation(
            @RequestBody SiuInvestigationActionRequest request,
            Authentication authentication) {
        try {
            log.info("SIU investigator '{}' starting investigation for claim ID: {}",
                    authentication.getName(), request.getClaimId());

            SiuInvestigationActionResponse response = siuService.startInvestigation(request, authentication.getName());

            if (response.isSuccess()) {
                log.info("Investigation started successfully for claim {} by investigator {}",
                        request.getClaimId(), authentication.getName());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to start investigation for claim {} by investigator {}: {}",
                        request.getClaimId(), authentication.getName(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Unexpected error starting investigation for claim {} by investigator '{}': {}",
                     request.getClaimId(), authentication.getName(), e.getMessage(), e);

            SiuInvestigationActionResponse errorResponse = SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "System error occurred while starting investigation. Please try again."
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Mark claim as fraudulent after investigation.
     * Changes claim status to rejected with fraud indication.
     *
     * @param request Investigation action request containing claim ID and fraud reason
     * @param authentication Current user authentication context
     * @return Investigation action response with success status and updated claim status
     */
    @PostMapping("/mark-fraud")
    public ResponseEntity<SiuInvestigationActionResponse> markAsFraud(
            @RequestBody SiuInvestigationActionRequest request,
            Authentication authentication) {
        try {
            log.info("SIU investigator '{}' marking claim {} as fraudulent",
                    authentication.getName(), request.getClaimId());

            SiuInvestigationActionResponse response = siuService.markAsFraud(request, authentication.getName());

            if (response.isSuccess()) {
                log.info("Claim {} successfully marked as fraudulent by investigator {}",
                        request.getClaimId(), authentication.getName());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to mark claim {} as fraudulent by investigator {}: {}",
                        request.getClaimId(), authentication.getName(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Unexpected error marking claim {} as fraud by investigator '{}': {}",
                     request.getClaimId(), authentication.getName(), e.getMessage(), e);

            SiuInvestigationActionResponse errorResponse = SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "System error occurred while marking claim as fraudulent. Please try again."
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Clear claim as legitimate after investigation.
     * Changes claim status to approved indicating no fraud detected.
     *
     * @param request Investigation action request containing claim ID and clearance notes
     * @param authentication Current user authentication context
     * @return Investigation action response with success status and updated claim status
     */
    @PostMapping("/clear-claim")
    public ResponseEntity<SiuInvestigationActionResponse> clearClaim(
            @RequestBody SiuInvestigationActionRequest request,
            Authentication authentication) {
        try {
            log.info("SIU investigator '{}' clearing claim {} as legitimate",
                    authentication.getName(), request.getClaimId());

            SiuInvestigationActionResponse response = siuService.clearClaim(request, authentication.getName());

            if (response.isSuccess()) {
                log.info("Claim {} successfully cleared as legitimate by investigator {}",
                        request.getClaimId(), authentication.getName());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to clear claim {} by investigator {}: {}",
                        request.getClaimId(), authentication.getName(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Unexpected error clearing claim {} by investigator '{}': {}",
                     request.getClaimId(), authentication.getName(), e.getMessage(), e);

            SiuInvestigationActionResponse errorResponse = SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "System error occurred while clearing claim. Please try again."
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}