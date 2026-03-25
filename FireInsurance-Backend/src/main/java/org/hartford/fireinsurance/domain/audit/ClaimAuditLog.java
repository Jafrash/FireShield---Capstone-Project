package org.hartford.fireinsurance.domain.audit;

import jakarta.persistence.*;
import org.hartford.fireinsurance.domain.state.ClaimState;

import java.time.LocalDateTime;

/**
 * Complete audit log entity for tracking all claim-related activities.
 * Provides comprehensive compliance trail for regulatory requirements.
 */
// @Entity // Disabled to resolve duplicate entity name error
// @Table(name = "claim_audit_log", indexes = {
//     @Index(name = "idx_claim_audit_claim_id", columnList = "claim_id"),
    // @Index(name = "idx_claim_audit_event_type", columnList = "event_type"),
    // @Index(name = "idx_claim_audit_performed_at", columnList = "performed_at"),
    // @Index(name = "idx_claim_audit_performed_by", columnList = "performed_by")
// })
public class ClaimAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;

    @Column(name = "from_state")
    @Enumerated(EnumType.STRING)
    private ClaimState fromState;

    @Column(name = "to_state")
    @Enumerated(EnumType.STRING)
    private ClaimState toState;

    @Column(name = "event_details", columnDefinition = "TEXT")
    private String eventDetails; // JSON payload with full context

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    // Security and compliance tracking
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "session_id")
    private String sessionId;

    // Compliance fields
    @Column(name = "compliance_flags")
    private String complianceFlags; // JSON array of compliance requirements met

    @Column(name = "regulatory_notes", columnDefinition = "TEXT")
    private String regulatoryNotes;

    @Column(name = "risk_level")
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    // Default constructor for JPA
    protected ClaimAuditLog() {
    }

    private ClaimAuditLog(Builder builder) {
        this.claimId = builder.claimId;
        this.eventType = builder.eventType;
        this.fromState = builder.fromState;
        this.toState = builder.toState;
        this.eventDetails = builder.eventDetails;
        this.performedBy = builder.performedBy;
        this.performedAt = builder.performedAt;
        this.ipAddress = builder.ipAddress;
        this.userAgent = builder.userAgent;
        this.sessionId = builder.sessionId;
        this.complianceFlags = builder.complianceFlags;
        this.regulatoryNotes = builder.regulatoryNotes;
        this.riskLevel = builder.riskLevel;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getClaimId() {
        return claimId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public ClaimState getFromState() {
        return fromState;
    }

    public ClaimState getToState() {
        return toState;
    }

    public String getEventDetails() {
        return eventDetails;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public LocalDateTime getPerformedAt() {
        return performedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getComplianceFlags() {
        return complianceFlags;
    }

    public String getRegulatoryNotes() {
        return regulatoryNotes;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    /**
     * Builder pattern for creating audit log entries.
     */
    public static class Builder {
        private Long claimId;
        private AuditEventType eventType;
        private ClaimState fromState;
        private ClaimState toState;
        private String eventDetails;
        private String performedBy;
        private LocalDateTime performedAt = LocalDateTime.now();
        private String ipAddress;
        private String userAgent;
        private String sessionId;
        private String complianceFlags;
        private String regulatoryNotes;
        private RiskLevel riskLevel;

        public Builder claimId(Long claimId) {
            this.claimId = claimId;
            return this;
        }

        public Builder eventType(AuditEventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder fromState(ClaimState fromState) {
            this.fromState = fromState;
            return this;
        }

        public Builder toState(ClaimState toState) {
            this.toState = toState;
            return this;
        }

        public Builder eventDetails(String eventDetails) {
            this.eventDetails = eventDetails;
            return this;
        }

        public Builder performedBy(String performedBy) {
            this.performedBy = performedBy;
            return this;
        }

        public Builder performedAt(LocalDateTime performedAt) {
            this.performedAt = performedAt;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder complianceFlags(String complianceFlags) {
            this.complianceFlags = complianceFlags;
            return this;
        }

        public Builder regulatoryNotes(String regulatoryNotes) {
            this.regulatoryNotes = regulatoryNotes;
            return this;
        }

        public Builder riskLevel(RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public ClaimAuditLog build() {
            return new ClaimAuditLog(this);
        }
    }

    /**
     * Enum for audit event types.
     */
    public enum AuditEventType {
        STATE_TRANSITION("State transition"),
        ASSIGNMENT_CHANGE("Assignment change"),
        FRAUD_SCORE_UPDATE("Fraud score update"),
        INVESTIGATION_ACTION("Investigation action"),
        DOCUMENT_UPLOAD("Document upload"),
        MANUAL_OVERRIDE("Manual override"),
        SYSTEM_ACTION("System action"),
        SECURITY_EVENT("Security event"),
        COMPLIANCE_CHECK("Compliance check");

        private final String description;

        AuditEventType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Enum for risk levels in audit context.
     */
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}

