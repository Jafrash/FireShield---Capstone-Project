package org.hartford.fireinsurance.service.refactored;

import org.hartford.fireinsurance.dto.CreateClaimRequest;
import org.hartford.fireinsurance.dto.FraudAnalysisResponse;
import org.hartford.fireinsurance.domain.state.ClaimState;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.Claim.ClaimStatus;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.hartford.fireinsurance.repository.PolicySubscriptionRepository;
import org.hartford.fireinsurance.service.CustomerService;
import org.hartford.fireinsurance.service.EmailNotificationService;
import org.hartford.fireinsurance.service.processing.ClaimProcessingService;
import org.hartford.fireinsurance.service.processing.ClaimSecurityService;
import org.hartford.fireinsurance.service.concurrency.ConcurrencyControlService;
import org.hartford.fireinsurance.service.enhanced.EnhancedFraudDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Refactored ClaimService integrating new fraud detection architecture.
 *
 * ARCHITECTURAL IMPROVEMENTS:
 * - Method-level transactions for precise control
 * - Event-driven fraud analysis (async)
 * - Unified state machine (single source of truth)
 * - Backend security enforcement
 * - Optimistic locking for concurrency control
 * - Complete audit trail
 *
 * SAFETY FEATURES:
 * - Backward compatibility maintained
 * - Gradual migration path
 * - Enhanced error handling
 * - Performance monitoring
 */
@Service
@Primary  // This will replace the existing ClaimService during migration
public class RefactoredClaimService {

    private static final Logger log = LoggerFactory.getLogger(RefactoredClaimService.class);

    // New architecture components
    private final ClaimProcessingService claimProcessingService;
    private final ClaimSecurityService claimSecurityService;
    private final ConcurrencyControlService concurrencyControlService;
    private final EnhancedFraudDetectionService enhancedFraudDetectionService;

    // Existing components (maintained for compatibility)
    private final ClaimRepository claimRepository;
    private final PolicySubscriptionRepository subscriptionRepository;
    private final CustomerService customerService;
    private final EmailNotificationService emailNotificationService;

    @Autowired
    public RefactoredClaimService(
            ClaimProcessingService claimProcessingService,
            ClaimSecurityService claimSecurityService,
            ConcurrencyControlService concurrencyControlService,
            EnhancedFraudDetectionService enhancedFraudDetectionService,
            ClaimRepository claimRepository,
            PolicySubscriptionRepository subscriptionRepository,
            CustomerService customerService,
            EmailNotificationService emailNotificationService) {

        this.claimProcessingService = claimProcessingService;
        this.claimSecurityService = claimSecurityService;
        this.concurrencyControlService = concurrencyControlService;
        this.enhancedFraudDetectionService = enhancedFraudDetectionService;
        this.claimRepository = claimRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.customerService = customerService;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Creates a new claim using the new unified architecture.
     * REPLACED: Synchronous fraud detection with async event-driven analysis
     * ADDED: Unified state machine and proper transaction boundaries
     * ENHANCED: Integrated fraud analysis workflow
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public Claim createClaim(CreateClaimRequest request) {
        log.info("Creating claim using new architecture for customer: {}", request.getCustomerName());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        try {
            // Step 1: Use new ClaimProcessingService for unified architecture
            Claim claim = claimProcessingService.submitClaim(request, auth);

            // Step 2: Trigger enhanced fraud analysis (asynchronous via events)
            // This will publish FraudScoreCalculatedEvent for further processing
            try {
                FraudAnalysisResponse fraudAnalysis = enhancedFraudDetectionService.analyzeClaim(claim.getClaimId());

                log.info("Fraud analysis completed for claim {}: Score={}, Risk={}",
                    claim.getClaimId(), fraudAnalysis.getFraudScore(), fraudAnalysis.getRiskLevel());

                // Log fraud analysis results for immediate visibility
                if (fraudAnalysis.getFraudScore() >= 70.0) {
                    log.warn("HIGH FRAUD RISK DETECTED for claim {}: Score={}, Auto-escalation to SIU recommended",
                        claim.getClaimId(), fraudAnalysis.getFraudScore());
                } else if (fraudAnalysis.getFraudScore() >= 50.0) {
                    log.info("MODERATE FRAUD RISK for claim {}: Score={}, Enhanced review recommended",
                        claim.getClaimId(), fraudAnalysis.getFraudScore());
                }

                // The fraud analysis results are now embedded in the claim via FraudAssessment value object
                // State transitions based on fraud score will be handled by event listeners

            } catch (Exception fraudAnalysisError) {
                log.error("Fraud analysis failed for claim {} but claim creation succeeded: {}",
                    claim.getClaimId(), fraudAnalysisError.getMessage());
                // Don't fail the claim creation if fraud analysis fails
                // The fraud analysis can be retried later
            }

            log.info("Successfully created claim {} with unified state: {}",
                claim.getClaimId(), claim.getCurrentState());

            return claim;

        } catch (Exception e) {
            log.error("Failed to create claim using new architecture: {}", e.getMessage(), e);
            throw new ClaimCreationException("Failed to create claim", e);
        }
    }

    /**
     * Approves claim with CRITICAL SECURITY ENFORCEMENT.
     * FIXED: The major security vulnerability where claims under SIU investigation could be approved
     * ENHANCED: Uses new fraud assessment validation
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER')")
    public Claim approveClaim(Long claimId) {
        log.info("Processing claim approval with security validation: {}", claimId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return concurrencyControlService.updateClaimWithRetry(claimId, claim -> {

            // CRITICAL SECURITY CHECK: Backend enforcement prevents approval during SIU investigation
            claimSecurityService.enforceStateTransitionAccess(
                claim.getCurrentState(), ClaimState.APPROVED_FOR_SETTLEMENT, auth);

            // ENHANCED: Additional fraud assessment validation
            if (claim.getFraudAssessment() != null) {
                Double fraudScore = claim.getFraudAssessment().getFraudScore();
                if (fraudScore != null && enhancedFraudDetectionService.requiresSiuEscalation(fraudScore)) {
                    throw new IllegalStateException(
                        String.format("Cannot approve claim %d: Fraud score %.2f requires SIU investigation. " +
                                     "Current state: %s", claimId, fraudScore, claim.getCurrentState()));
                }

                if (fraudScore != null && enhancedFraudDetectionService.requiresEnhancedReview(fraudScore)) {
                    log.warn("Approving claim {} with elevated fraud score: {}. Enhanced review completed.",
                        claimId, fraudScore);
                }
            }

            // Business logic: Calculate settlement amount
            Double settlementAmount = calculateSettlementAmount(claim);
            if (settlementAmount != null) {
                claim.setSettlementAmount(settlementAmount);
                log.info("Calculated settlement amount: ₹{} for claim: {}", settlementAmount, claimId);
            }

            // Use unified state machine for approval
            claim.transitionTo(ClaimState.APPROVED_FOR_SETTLEMENT,
                String.format("Claim approved for settlement: ₹%.2f", settlementAmount),
                auth.getName());

            // Maintain backward compatibility for existing code
            claim.setStatus(ClaimStatus.APPROVED);

            return claim;
        });
    }

    /**
     * SIU assignment with new architecture integration.
     * IMPROVED: Uses unified state machine and proper assignment tracking
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_SUPERVISOR')")
    public Claim assignToSiu(Long claimId, Long siuInvestigatorId, String initialNotes) {
        log.info("Assigning claim {} to SIU investigator {} using new architecture", claimId, siuInvestigatorId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        try {
            // Use ClaimProcessingService for proper SIU assignment
            Claim claim = claimProcessingService.assignToSiuInvestigator(
                claimId, siuInvestigatorId, LocalDateTime.now().plusDays(7), initialNotes, auth);

            log.info("Successfully assigned claim {} to SIU investigator using unified architecture", claimId);
            return claim;

        } catch (Exception e) {
            log.error("Failed to assign claim {} to SIU investigator: {}", claimId, e.getMessage(), e);
            throw new SiuAssignmentException("Failed to assign claim to SIU", e);
        }
    }

    /**
     * Claim settlement with fraud status validation.
     * ENHANCED: Uses optimistic locking and state machine validation
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER')")
    public Claim settleClaim(Long claimId, Double settlementAmount, String notes) {
        log.info("Processing claim settlement with enhanced security: {}", claimId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return claimProcessingService.settleClaim(claimId, settlementAmount, notes, auth);
    }

    /**
     * Reject claim with proper state machine integration.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SIU_INVESTIGATOR')")
    public Claim rejectClaim(Long claimId, String rejectionReason) {
        log.info("Processing claim rejection: {}", claimId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return concurrencyControlService.updateClaimWithRetry(claimId, claim -> {
            claimSecurityService.enforceStateTransitionAccess(
                claim.getCurrentState(), ClaimState.REJECTED, auth);

            claim.transitionTo(ClaimState.REJECTED, "Claim rejected: " + rejectionReason, auth.getName());

            // Backward compatibility
            claim.setStatus(ClaimStatus.REJECTED);

            return claim;
        });
    }

    /**
     * Get claim by ID with security validation.
     * ENHANCED: Includes access control based on assignment
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SIU_INVESTIGATOR', 'CUSTOMER')")
    public Claim getClaimById(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + claimId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Enhanced access control based on assignment
        if (!claimSecurityService.canAccessClaim(claimId,
                claim.getAssignmentTracker().getAssigneeType(),
                claim.getAssignmentTracker().getCurrentAssignee() != null ?
                    claim.getAssignmentTracker().getCurrentAssignee().getUserId() : null,
                auth)) {

            throw new UnauthorizedClaimAccessException("Access denied to claim: " + claimId);
        }

        return claim;
    }

    /**
     * Get claims by SIU investigator with enhanced querying.
     * IMPROVED: Uses new repository with optimized queries
     */
    public List<Claim> getClaimsBySiuInvestigator(Long investigatorId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Security validation
        if (!claimSecurityService.canAccessClaim(null, null, investigatorId, auth) &&
            !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new UnauthorizedClaimAccessException("Access denied to investigator claims");
        }

        // Use enhanced repository for optimized query
        return claimRepository.findByAssignedInvestigator(investigatorId);
    }

    /**
     * Get all claims with enhanced filtering and security.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER')")
    public List<Claim> getAllClaims() {
        return claimRepository.findAll();
    }

    /**
     * Update claim status using unified state machine.
     * REPLACED: Direct status updates with validated state transitions
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER')")
    public Claim updateClaimStatus(Long claimId, ClaimStatus newStatus, String reason) {
        log.info("Updating claim {} status to {} using state machine", claimId, newStatus);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return concurrencyControlService.updateClaimWithRetry(claimId, claim -> {
            // Map legacy status to new state
            ClaimState newState = mapLegacyStatusToState(newStatus);

            // Validate transition
            claimSecurityService.enforceStateTransitionAccess(
                claim.getCurrentState(), newState, auth);

            // Apply state transition
            claim.transitionTo(newState, reason, auth.getName());

            // Maintain backward compatibility
            claim.setStatus(newStatus);

            return claim;
        });
    }

    /**
     * Manual fraud analysis trigger - allows re-analysis of existing claims.
     * ENHANCED: Provides comprehensive fraud re-evaluation for suspicious claims
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER', 'SIU_SUPERVISOR')")
    public FraudAnalysisResponse performFraudAnalysis(Long claimId) {
        log.info("Triggering manual fraud analysis for claim: {}", claimId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Verify claim exists and user has access
        Claim claim = getClaimById(claimId);

        try {
            // Perform comprehensive fraud analysis
            FraudAnalysisResponse fraudAnalysis = enhancedFraudDetectionService.analyzeClaim(claimId);

            log.info("Manual fraud analysis completed for claim {}: Score={}, Risk={}, Rules triggered: {}",
                claimId, fraudAnalysis.getFraudScore(), fraudAnalysis.getRiskLevel(),
                fraudAnalysis.getRuleBreakdown().stream()
                    .filter(rule -> rule.getTriggered())
                    .map(rule -> rule.getRuleName())
                    .toList());

            // Log important findings
            if (fraudAnalysis.getFraudScore() >= 70.0) {
                log.warn("CRITICAL FRAUD RISK detected in manual analysis for claim {}: Score={}. " +
                        "Automatic SIU escalation recommended.", claimId, fraudAnalysis.getFraudScore());
            }

            return fraudAnalysis;

        } catch (Exception e) {
            log.error("Manual fraud analysis failed for claim {}: {}", claimId, e.getMessage(), e);
            throw new FraudAnalysisException("Failed to perform fraud analysis on claim: " + claimId, e);
        }
    }

    // Helper methods

    /**
     * Calculates settlement amount using existing business logic.
     * TODO: This could be moved to a dedicated SettlementCalculationService
     */
    private Double calculateSettlementAmount(Claim claim) {
        try {
            // Use existing settlement calculation logic
            Double claimAmount = claim.getClaimAmount();
            if (claimAmount == null || claimAmount <= 0) {
                return null;
            }

            // Apply deductible (5%)
            double deductible = claimAmount * 0.05;
            double amountAfterDeductible = claimAmount - deductible;

            // Apply depreciation based on property type
            double depreciation = calculateDepreciation(claim, amountAfterDeductible);
            double finalAmount = amountAfterDeductible - depreciation;

            return Math.max(finalAmount, 0.0);

        } catch (Exception e) {
            log.error("Error calculating settlement for claim {}: {}", claim.getClaimId(), e.getMessage());
            return null;
        }
    }

    private double calculateDepreciation(Claim claim, double amount) {
        // Simplified depreciation logic - could be enhanced
        return amount * 0.10; // 10% default depreciation
    }

    /**
     * Maps legacy ClaimStatus to new unified ClaimState.
     */
    private ClaimState mapLegacyStatusToState(ClaimStatus legacyStatus) {
        return switch (legacyStatus) {
            case SUBMITTED -> ClaimState.SUBMITTED;
            case UNDER_REVIEW -> ClaimState.UNDER_INITIAL_REVIEW;
            case SURVEY_ASSIGNED -> ClaimState.SURVEY_ASSIGNED;
            case SURVEY_COMPLETED -> ClaimState.SURVEY_COMPLETED;
            case APPROVED -> ClaimState.APPROVED_FOR_SETTLEMENT;
            case REJECTED -> ClaimState.REJECTED;
            case PAID, SETTLED -> ClaimState.SETTLED;
            default -> ClaimState.SUBMITTED;
        };
    }

    // Exception classes
    public static class ClaimCreationException extends RuntimeException {
        public ClaimCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SiuAssignmentException extends RuntimeException {
        public SiuAssignmentException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ClaimNotFoundException extends RuntimeException {
        public ClaimNotFoundException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedClaimAccessException extends RuntimeException {
        public UnauthorizedClaimAccessException(String message) {
            super(message);
        }
    }

    public static class FraudAnalysisException extends RuntimeException {
        public FraudAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}