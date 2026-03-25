package org.hartford.fireinsurance.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.hartford.fireinsurance.domain.event.ClaimStateTransitionEvent;
import org.hartford.fireinsurance.domain.event.FraudScoreCalculatedEvent;
import org.hartford.fireinsurance.model.ClaimAuditLog;
import org.hartford.fireinsurance.repository.audit.ClaimAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for comprehensive audit logging in the fraud detection system.
 * Automatically captures domain events and provides manual audit capabilities.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final ClaimAuditLogRepository auditRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditService(ClaimAuditLogRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Automatically captures claim state transition events.
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditStateTransition(ClaimStateTransitionEvent event) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("claimId", event.getClaimId());
            eventData.put("fromState", event.getFromState().name());
            eventData.put("toState", event.getToState().name());
            eventData.put("reason", event.getReason());
            eventData.put("eventId", event.getEventId());
            eventData.put("correlationId", event.getCorrelationId());
            eventData.put("isEscalation", event.isEscalationToFraud());
            eventData.put("isSettlement", event.isSettlement());

            ClaimAuditLog auditLog = ClaimAuditLog.builder(event.getClaimId(), ClaimAuditLog.AuditEventType.STATE_TRANSITION)
                .fromState(event.getFromState())
                .toState(event.getToState())
                .eventDetails(objectMapper.writeValueAsString(eventData))
                .performedBy(event.getModifiedBy())
                .performedAt(event.getOccurredAt())
                .ipAddress(getCurrentIpAddress())
                .userAgent(getCurrentUserAgent())
                .sessionId(getCurrentSessionId())
                .riskLevel(determineRiskLevel(event))
                .build();

            auditRepository.save(auditLog);

            log.info("Audited state transition for claim {}: {} -> {}",
                event.getClaimId(), event.getFromState(), event.getToState());

        } catch (Exception e) {
            log.error("Failed to audit state transition event for claim {}: {}",
                event.getClaimId(), e.getMessage(), e);
            // Don't re-throw to avoid breaking the main transaction
        }
    }

    /**
     * Automatically captures fraud score calculation events.
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditFraudScoreUpdate(FraudScoreCalculatedEvent event) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("claimId", event.getClaimId());
            eventData.put("fraudScore", event.getFraudScore());
            eventData.put("riskLevel", event.getRiskLevel().name());
            eventData.put("triggeredRules", event.getTriggeredRules());
            eventData.put("analysisVersion", event.getAnalysisVersion());
            eventData.put("requiresSiuEscalation", event.requiresSiuEscalation());
            eventData.put("hasBlacklistViolations", event.hasBlacklistViolations());

            ClaimAuditLog auditLog = ClaimAuditLog.builder(event.getClaimId(), ClaimAuditLog.AuditEventType.FRAUD_SCORE_UPDATE)
                .eventDetails(objectMapper.writeValueAsString(eventData))
                .performedBy("SYSTEM_FRAUD_ENGINE")
                .performedAt(event.getOccurredAt())
                .riskLevel(mapRiskLevel(event.getRiskLevel()))
                .complianceFlags(generateComplianceFlags(event))
                .build();

            auditRepository.save(auditLog);

            log.info("Audited fraud score update for claim {}: score={}, level={}",
                event.getClaimId(), event.getFraudScore(), event.getRiskLevel());

        } catch (Exception e) {
            log.error("Failed to audit fraud score event for claim {}: {}",
                event.getClaimId(), e.getMessage(), e);
        }
    }

    /**
     * Manual audit logging for security events.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditSecurityEvent(Long claimId, String eventDetails, String auditReason) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : "UNKNOWN";

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("securityEvent", eventDetails);
            eventData.put("reason", auditReason);
            eventData.put("userRoles", auth != null ? auth.getAuthorities() : "N/A");

            ClaimAuditLog auditLog = ClaimAuditLog.builder(claimId, ClaimAuditLog.AuditEventType.SECURITY_EVENT)
                .eventDetails(objectMapper.writeValueAsString(eventData))
                .performedBy(username)
                .ipAddress(getCurrentIpAddress())
                .userAgent(getCurrentUserAgent())
                .sessionId(getCurrentSessionId())
                .riskLevel(ClaimAuditLog.RiskLevel.HIGH)
                .regulatoryNotes("Security event - requires review")
                .build();

            auditRepository.save(auditLog);

            log.warn("Security event audited for claim {}: {} by user {}",
                claimId, eventDetails, username);

        } catch (Exception e) {
            log.error("Failed to audit security event for claim {}: {}", claimId, e.getMessage(), e);
        }
    }

    /**
     * Gets current IP address from HTTP request.
     */
    private String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not determine IP address: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    /**
     * Gets current user agent from HTTP request.
     */
    private String getCurrentUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Could not determine user agent: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    /**
     * Gets current session ID from HTTP request.
     */
    private String getCurrentSessionId() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getSession(false) != null ?
                    attrs.getRequest().getSession().getId() : null;
            }
        } catch (Exception e) {
            log.debug("Could not determine session ID: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Determines audit risk level based on state transition.
     */
    private ClaimAuditLog.RiskLevel determineRiskLevel(ClaimStateTransitionEvent event) {
        if (event.isEscalationToFraud()) {
            return ClaimAuditLog.RiskLevel.HIGH;
        } else if (event.isFraudInvestigationComplete()) {
            return ClaimAuditLog.RiskLevel.CRITICAL;
        } else if (event.isSettlement()) {
            return ClaimAuditLog.RiskLevel.MEDIUM;
        }
        return ClaimAuditLog.RiskLevel.LOW;
    }

    /**
     * Maps domain risk level to audit risk level.
     */
    private ClaimAuditLog.RiskLevel mapRiskLevel(org.hartford.fireinsurance.model.Claim.RiskLevel riskLevel) {
        return switch (riskLevel) {
            case NEGLIGIBLE -> ClaimAuditLog.RiskLevel.LOW;  // Map NEGLIGIBLE to LOW for audit purposes
            case LOW -> ClaimAuditLog.RiskLevel.LOW;
            case MEDIUM -> ClaimAuditLog.RiskLevel.MEDIUM;
            case HIGH -> ClaimAuditLog.RiskLevel.HIGH;
            case CRITICAL -> ClaimAuditLog.RiskLevel.CRITICAL;
        };
    }

    /**
     * Generates compliance flags based on fraud analysis.
     */
    private String generateComplianceFlags(FraudScoreCalculatedEvent event) {
        try {
            Map<String, Boolean> flags = new HashMap<>();
            flags.put("FRAUD_ANALYSIS_COMPLETED", true);
            flags.put("AUTO_ESCALATION_TRIGGERED", event.requiresSiuEscalation());
            flags.put("BLACKLIST_CHECK_PERFORMED", event.hasBlacklistViolations());
            flags.put("ENHANCED_REVIEW_REQUIRED", event.requiresEnhancedReview());

            return objectMapper.writeValueAsString(flags);
        } catch (Exception e) {
            log.warn("Could not generate compliance flags: {}", e.getMessage());
            return "{}";
        }
    }
}