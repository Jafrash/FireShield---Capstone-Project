package org.hartford.fireinsurance.service.processing;

import org.hartford.fireinsurance.domain.event.*;
import org.hartford.fireinsurance.domain.state.ClaimState;
import org.hartford.fireinsurance.domain.valueobject.AssignmentTracker;
import org.hartford.fireinsurance.domain.valueobject.FraudAssessment;
import org.hartford.fireinsurance.dto.CreateClaimRequest;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.hartford.fireinsurance.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Core claim processing service with proper transaction boundaries.
 * Orchestrates all claim operations using the new unified architecture.
 *
 * SAFETY FEATURES:
 * - Method-level transactions for precise control
 * - Event-driven architecture for loose coupling
 * - State machine validation for business rules
 * - Comprehensive audit trail
 * - Backend security enforcement
 */
@Service
public class ClaimProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ClaimProcessingService.class);

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final DomainEventPublisher eventPublisher;
    private final ClaimSecurityService securityService;

    @Autowired
    public ClaimProcessingService(ClaimRepository claimRepository,
                                UserRepository userRepository,
                                DomainEventPublisher eventPublisher,
                                ClaimSecurityService securityService) {
        this.claimRepository = claimRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.securityService = securityService;
    }

    /**
     * Submits a new claim with unified state management and automatic fraud analysis.
     *
     * @param request claim submission data
     * @return created claim with initial state
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public Claim submitClaim(CreateClaimRequest request, Authentication auth) {
        log.info("Processing new claim submission for customer: {}", request.getCustomerName());

        try {
            // 1. Create claim with unified state
            Claim claim = createClaimFromRequest(request, auth.getName());

            // 2. Set initial state
            claim.transitionTo(ClaimState.SUBMITTED, "Initial claim submission", auth.getName());

            // 3. Embedded value objects are auto-initialized by JPA

            // 4. Save claim (atomic operation)
            claim = claimRepository.save(claim);

            // 5. Publish domain event for async processing (fraud analysis, notifications)
            eventPublisher.publish(new ClaimSubmittedEvent(
                claim.getClaimId(),
                request.getCustomerName(),
                request.getClaimAmount(),
                "FIRE_INSURANCE", // TODO: extract from request
                auth.getName()
            ));

            log.info("Successfully created claim {} in state: {}",
                claim.getClaimId(), claim.getCurrentState());

            return claim;

        } catch (Exception e) {
            log.error("Failed to submit claim for customer {}: {}",
                request.getCustomerName(), e.getMessage(), e);
            throw new ClaimProcessingException("Failed to submit claim", e);
        }
    }

    /**
     * Processes initial claim review with fraud-score driven routing.
     *
     * BUSINESS LOGIC:
     * - Fraud Score >= 70: Auto-escalate to SIU
     * - Fraud Score 50-69: Enhanced review required
     * - Fraud Score < 50: Normal processing
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER')")
    public Claim processInitialReview(Long claimId, String reviewNotes, Authentication auth) {
        log.info("Processing initial review for claim: {}", claimId);

        // 1. Get claim with optimistic locking
        Claim claim = claimRepository.findByIdWithLock(claimId)
            .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + claimId));

        // 2. Validate current state allows review
        if (!claim.getCurrentState().canTransitionTo(ClaimState.UNDER_INITIAL_REVIEW)) {
            throw new IllegalStateTransitionException(
                "Cannot review claim in current state: " + claim.getCurrentState());
        }

        // 3. Transition to review state
        claim.transitionTo(ClaimState.UNDER_INITIAL_REVIEW,
            "Initial review started: " + reviewNotes, auth.getName());

        // 4. Apply fraud-score driven business rules
        FraudAssessment fraudAssessment = claim.getFraudAssessment();

        ClaimState nextState;
        String reason;

        if (fraudAssessment.requiresSiuInvestigation()) {
            nextState = ClaimState.ESCALATED_TO_SIU;
            reason = String.format("Auto-escalated due to high fraud score: %.2f",
                fraudAssessment.getFraudScore());
        } else if (fraudAssessment.requiresEnhancedReview()) {
            nextState = ClaimState.FLAGGED_FOR_REVIEW;
            reason = String.format("Flagged for enhanced review due to fraud score: %.2f",
                fraudAssessment.getFraudScore());
        } else {
            nextState = ClaimState.APPROVED_FOR_SURVEY;
            reason = "Initial review passed - approved for survey";
        }

        // 5. Execute state transition
        claim.transitionTo(nextState, reason, auth.getName());

        // 6. Save with optimistic locking
        claim = claimRepository.save(claim);

        log.info("Claim {} reviewed and transitioned to: {}", claimId, nextState);
        return claim;
    }

    /**
     * Assigns claim to SIU investigator with proper security validation.
     *
     * SAFETY RULES:
     * - Only one active assignment per claim
     * - SIU assignments take precedence over other assignments
     * - Backend security validation enforced
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_SUPERVISOR')")
    public Claim assignToSiuInvestigator(Long claimId, Long investigatorId,
                                       LocalDateTime dueDate, String assignmentNotes,
                                       Authentication auth) {
        log.info("Assigning claim {} to SIU investigator: {}", claimId, investigatorId);

        // 1. Get claim with pessimistic lock to prevent concurrent modifications
        Claim claim = claimRepository.findByIdWithLock(claimId)
            .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + claimId));

        // 2. Backend security validation
        securityService.enforceStateTransitionAccess(
            claim.getCurrentState(), ClaimState.SIU_ASSIGNED, auth);

        // 3. Validate current state allows SIU assignment
        if (!claim.getCurrentState().canTransitionTo(ClaimState.SIU_ASSIGNED)) {
            throw new IllegalStateTransitionException(
                "Cannot assign SIU investigator in current state: " + claim.getCurrentState());
        }

        // 4. Get and validate investigator
        User investigator = userRepository.findById(investigatorId)
            .orElseThrow(() -> new UserNotFoundException("SIU Investigator not found: " + investigatorId));

        if (!investigator.hasRole("SIU_INVESTIGATOR")) {
            throw new InvalidAssignmentException("User is not an SIU investigator: " + investigatorId);
        }

        // 5. Clear existing assignment (SIU takes precedence)
        AssignmentTracker assignmentTracker = claim.getAssignmentTracker();
        if (assignmentTracker.hasActiveAssignment()) {
            log.info("Clearing existing assignment for claim {} as SIU takes precedence", claimId);
            assignmentTracker.clearAssignment();
        }

        // 6. Assign to SIU investigator
        assignmentTracker.assignTo(investigator, AssignmentTracker.AssigneeType.SIU_INVESTIGATOR,
                                 dueDate, assignmentNotes);

        // 7. Transition claim state
        claim.transitionTo(ClaimState.SIU_ASSIGNED,
            "Assigned to SIU investigator: " + investigator.getUsername(), auth.getName());

        // 8. Save with version control
        claim = claimRepository.save(claim);

        // 9. Publish assignment event
        eventPublisher.publish(new ClaimAssignedEvent(
            claimId, investigator.getUsername(), "SIU_INVESTIGATOR",
            auth.getName(), LocalDateTime.now().plusDays(7), assignmentNotes));

        log.info("Successfully assigned claim {} to SIU investigator {}",
            claimId, investigator.getUsername());

        return claim;
    }

    /**
     * Starts SIU investigation with proper state management.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'SIU_INVESTIGATOR')")
    public Claim startSiuInvestigation(Long claimId, String investigationNotes, Authentication auth) {
        log.info("Starting SIU investigation for claim: {}", claimId);

        Claim claim = claimRepository.findByIdWithLock(claimId)
            .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + claimId));

        // Validate investigator has access to this claim
        if (!claim.getAssignmentTracker().isAssignedToType(AssignmentTracker.AssigneeType.SIU_INVESTIGATOR)) {
            throw new UnauthorizedAccessException("Claim not assigned to SIU investigator");
        }

        // Transition to investigation state
        claim.transitionTo(ClaimState.UNDER_SIU_INVESTIGATION,
            "SIU investigation started: " + investigationNotes, auth.getName());

        return claimRepository.save(claim);
    }

    /**
     * Settles claim with fraud status validation.
     * CRITICAL SECURITY: Cannot settle claims under SIU investigation.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER')")
    public Claim settleClaim(Long claimId, Double settlementAmount, String settlementNotes, Authentication auth) {
        log.info("Processing claim settlement: {} for amount: {}", claimId, settlementAmount);

        Claim claim = claimRepository.findByIdWithLock(claimId)
            .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + claimId));

        // CRITICAL SECURITY CHECK: Prevent settlement during SIU investigation
        if (claim.getCurrentState().blocksNormalProcessing()) {
            throw new SecurityException(
                String.format("SECURITY VIOLATION: Cannot settle claim %d in state %s. " +
                            "Investigation must complete first.", claimId, claim.getCurrentState()));
        }

        // Validate state transition
        if (!claim.getCurrentState().canTransitionTo(ClaimState.SETTLED)) {
            throw new IllegalStateTransitionException(
                "Cannot settle claim in current state: " + claim.getCurrentState());
        }

        // Transition to settled state
        claim.transitionTo(ClaimState.SETTLED,
            String.format("Claim settled for amount: %.2f. %s", settlementAmount, settlementNotes),
            auth.getName());

        return claimRepository.save(claim);
    }

    // Helper methods

    private Claim createClaimFromRequest(CreateClaimRequest request, String submittedBy) {
        // Implementation details for creating claim from request
        // This would use existing Claim constructor logic
        // TODO: Implement based on existing CreateClaimRequest structure
        throw new UnsupportedOperationException("To be implemented based on existing Claim creation logic");
    }

    private FraudAssessment createInitialFraudAssessment() {
        // Initial fraud assessment - will be updated by fraud analysis engine
        return new FraudAssessment(0.0, FraudAssessment.RiskLevel.LOW,
                                 "Initial assessment pending fraud analysis", "v1.0");
    }

    // Exception classes
    public static class ClaimProcessingException extends RuntimeException {
        public ClaimProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ClaimNotFoundException extends RuntimeException {
        public ClaimNotFoundException(String message) {
            super(message);
        }
    }

    public static class IllegalStateTransitionException extends RuntimeException {
        public IllegalStateTransitionException(String message) {
            super(message);
        }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidAssignmentException extends RuntimeException {
        public InvalidAssignmentException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedAccessException extends RuntimeException {
        public UnauthorizedAccessException(String message) {
            super(message);
        }
    }
}