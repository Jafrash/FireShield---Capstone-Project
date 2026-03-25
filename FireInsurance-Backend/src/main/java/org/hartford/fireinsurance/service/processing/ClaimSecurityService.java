package org.hartford.fireinsurance.service.processing;

import org.hartford.fireinsurance.domain.state.ClaimState;
import org.hartford.fireinsurance.domain.valueobject.AssignmentTracker.AssigneeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Backend security service for fraud detection system.
 * Enforces role-based access control (RBAC) at the business logic level.
 *
 * SECURITY PRINCIPLES:
 * - Backend enforcement - never trust UI-level validation
 * - Principle of least privilege - minimal required permissions
 * - Fail-secure - deny by default, explicit allow only
 * - Audit all security decisions
 */
@Service
public class ClaimSecurityService {

    private static final Logger log = LoggerFactory.getLogger(ClaimSecurityService.class);

    /**
     * Enforces access control for claim state transitions.
     * This is the CRITICAL security method that prevents unauthorized state changes.
     *
     * @param fromState current claim state
     * @param toState target claim state
     * @param auth authentication context
     * @throws AccessDeniedException if user lacks required permissions
     * @throws SecurityException for critical security violations
     */
    public void enforceStateTransitionAccess(ClaimState fromState, ClaimState toState, Authentication auth) {
        Set<String> userRoles = extractUserRoles(auth);
        String username = auth.getName();

        log.debug("Checking state transition access: {} -> {} for user: {} with roles: {}",
            fromState, toState, username, userRoles);

        // Critical security validations based on target state
        switch (toState) {

            case UNDER_INITIAL_REVIEW -> {
                requireRole(userRoles, "ROLE_UNDERWRITER", "ROLE_ADMIN");
                logSecurityDecision(username, "ALLOWED", "Initial review access", toState);
            }

            case ESCALATED_TO_SIU -> {
                requireRole(userRoles, "ROLE_ADMIN", "ROLE_UNDERWRITER", "ROLE_SIU_SUPERVISOR");
                logSecurityDecision(username, "ALLOWED", "SIU escalation access", toState);
            }

            case SIU_ASSIGNED -> {
                requireRole(userRoles, "ROLE_ADMIN", "ROLE_SIU_SUPERVISOR");
                logSecurityDecision(username, "ALLOWED", "SIU assignment access", toState);
            }

            case UNDER_SIU_INVESTIGATION -> {
                requireRole(userRoles, "ROLE_SIU_INVESTIGATOR", "ROLE_ADMIN");
                logSecurityDecision(username, "ALLOWED", "SIU investigation access", toState);
            }

            case SIU_INVESTIGATION_COMPLETED -> {
                requireRole(userRoles, "ROLE_SIU_INVESTIGATOR", "ROLE_ADMIN");
                logSecurityDecision(username, "ALLOWED", "Investigation completion access", toState);
            }

            case CONFIRMED_FRAUD -> {
                // CRITICAL: Only SIU investigators can confirm fraud
                requireRole(userRoles, "ROLE_SIU_INVESTIGATOR", "ROLE_ADMIN");
                logCriticalSecurityDecision(username, "FRAUD_CONFIRMATION", toState);
            }

            case BLACKLISTED -> {
                // CRITICAL: Blacklisting requires highest privileges
                requireRole(userRoles, "ROLE_ADMIN");
                logCriticalSecurityDecision(username, "BLACKLIST_ACTION", toState);
            }

            case APPROVED_FOR_SETTLEMENT, SETTLED -> {
                // CRITICAL SECURITY CHECK: Cannot settle during SIU investigation
                if (fromState == ClaimState.UNDER_SIU_INVESTIGATION
                    || fromState == ClaimState.SIU_ASSIGNED
                    || fromState == ClaimState.ESCALATED_TO_SIU) {

                    String violation = String.format(
                        "CRITICAL SECURITY VIOLATION: User %s attempted to approve/settle claim in state %s. " +
                        "This is a potential fraud bypass attempt.", username, fromState);

                    log.error(violation);
                    throw new SecurityException(violation);
                }

                requireRole(userRoles, "ROLE_UNDERWRITER", "ROLE_ADMIN");
                logSecurityDecision(username, "ALLOWED", "Settlement access", toState);
            }

            case REJECTED -> {
                requireRole(userRoles, "ROLE_UNDERWRITER", "ROLE_ADMIN", "ROLE_SIU_INVESTIGATOR");
                logSecurityDecision(username, "ALLOWED", "Rejection access", toState);
            }

            case CLEARED_BY_REVIEW, CLEARED_BY_SIU -> {
                if (toState == ClaimState.CLEARED_BY_SIU) {
                    requireRole(userRoles, "ROLE_SIU_INVESTIGATOR", "ROLE_ADMIN");
                } else {
                    requireRole(userRoles, "ROLE_UNDERWRITER", "ROLE_ADMIN");
                }
                logSecurityDecision(username, "ALLOWED", "Clearance access", toState);
            }

            default -> {
                requireRole(userRoles, "ROLE_ADMIN"); // Fail-secure: admin only for undefined transitions
                logSecurityDecision(username, "ALLOWED", "Default admin access", toState);
            }
        }
    }

    /**
     * Enforces access control for claim assignments.
     *
     * @param assigneeType type of assignee (UNDERWRITER, SIU_INVESTIGATOR, etc.)
     * @param auth authentication context
     */
    public void enforceAssignmentAccess(AssigneeType assigneeType, Authentication auth) {
        Set<String> userRoles = extractUserRoles(auth);
        String username = auth.getName();

        log.debug("Checking assignment access for type: {} by user: {} with roles: {}",
            assigneeType, username, userRoles);

        switch (assigneeType) {
            case SIU_INVESTIGATOR -> {
                requireRole(userRoles, "ROLE_ADMIN", "ROLE_SIU_SUPERVISOR");
                logSecurityDecision(username, "ALLOWED", "SIU assignment", null);
            }
            case SURVEYOR -> {
                requireRole(userRoles, "ROLE_UNDERWRITER", "ROLE_ADMIN");
                logSecurityDecision(username, "ALLOWED", "Surveyor assignment", null);
            }
            case UNDERWRITER -> {
                requireRole(userRoles, "ROLE_ADMIN");
                logSecurityDecision(username, "ALLOWED", "Underwriter assignment", null);
            }
            default -> {
                requireRole(userRoles, "ROLE_ADMIN"); // Fail-secure
                logSecurityDecision(username, "ALLOWED", "Default assignment", null);
            }
        }
    }

    /**
     * Validates if user can access specific claim data based on assignment.
     *
     * @param claimId claim identifier
     * @param assigneeType current assignment type
     * @param assignedUserId currently assigned user ID
     * @param auth authentication context
     * @return true if access is allowed
     */
    public boolean canAccessClaim(Long claimId, AssigneeType assigneeType, Long assignedUserId, Authentication auth) {
        Set<String> userRoles = extractUserRoles(auth);
        String username = auth.getName();

        // Admins can access all claims
        if (userRoles.contains("ROLE_ADMIN")) {
            return true;
        }

        // Users can access claims assigned to them
        // TODO: Implement user ID lookup from authentication
        // For now, assume username can be used to determine user ID

        // SIU investigators can access their assigned claims
        if (assigneeType == AssigneeType.SIU_INVESTIGATOR && userRoles.contains("ROLE_SIU_INVESTIGATOR")) {
            // TODO: Verify user ID matches assignedUserId
            return true;
        }

        // Underwriters can access their assigned claims
        if (assigneeType == AssigneeType.UNDERWRITER && userRoles.contains("ROLE_UNDERWRITER")) {
            // TODO: Verify user ID matches assignedUserId
            return true;
        }

        log.warn("Access denied for user {} to claim {} (assigned to type: {})",
            username, claimId, assigneeType);
        return false;
    }

    /**
     * Checks if user can perform investigation actions on a claim.
     */
    public boolean canPerformInvestigationActions(Long claimId, ClaimState currentState, Authentication auth) {
        Set<String> userRoles = extractUserRoles(auth);

        // Only SIU investigators and admins can perform investigation actions
        if (!userRoles.contains("ROLE_SIU_INVESTIGATOR") && !userRoles.contains("ROLE_ADMIN")) {
            return false;
        }

        // Investigation actions only allowed in investigation states
        return currentState.isFraudRelated();
    }

    // Helper methods

    private Set<String> extractUserRoles(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    }

    private void requireRole(Set<String> userRoles, String... requiredRoles) {
        if (Arrays.stream(requiredRoles).noneMatch(userRoles::contains)) {
            String message = String.format("Access denied. Required roles: %s, User roles: %s",
                Arrays.toString(requiredRoles), userRoles);
            log.warn(message);
            throw new AccessDeniedException("Insufficient privileges for this operation");
        }
    }

    private void logSecurityDecision(String username, String decision, String action, ClaimState state) {
        log.info("SECURITY: {} - User: {}, Action: {}, Target State: {}",
            decision, username, action, state);
    }

    private void logCriticalSecurityDecision(String username, String actionType, ClaimState state) {
        log.warn("CRITICAL SECURITY ACTION: User {} performed {} -> {}", username, actionType, state);
        // TODO: Could trigger additional notifications or alerts for critical actions
    }

    /**
     * Validates fraud score access based on user role.
     */
    public boolean canAccessFraudScore(Authentication auth) {
        Set<String> userRoles = extractUserRoles(auth);
        return userRoles.contains("ROLE_ADMIN")
            || userRoles.contains("ROLE_UNDERWRITER")
            || userRoles.contains("ROLE_SIU_INVESTIGATOR");
    }

    /**
     * Validates access to detailed fraud analysis.
     */
    public boolean canAccessDetailedFraudAnalysis(Authentication auth) {
        Set<String> userRoles = extractUserRoles(auth);
        return userRoles.contains("ROLE_ADMIN")
            || userRoles.contains("ROLE_SIU_INVESTIGATOR");
    }

    /**
     * Checks if user can view audit logs for compliance.
     */
    public boolean canAccessAuditLogs(Authentication auth) {
        Set<String> userRoles = extractUserRoles(auth);
        return userRoles.contains("ROLE_ADMIN"); // Only admins can view full audit logs
    }
}