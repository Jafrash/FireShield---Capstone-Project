package org.hartford.fireinsurance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import org.hartford.fireinsurance.domain.state.ClaimState;

/**
 * Comprehensive audit log entity for tracking all claim-related activities.
 * This entity provides complete audit trail capabilities required for
 * regulatory compliance and forensic analysis.
 *
 * The audit log captures:
 * - All state transitions and business events
 * - User actions and system activities
 * - Fraud detection and investigation events
 * - Assignment changes and workflow decisions
 * - External system integrations
 *
 * This design ensures immutability of audit records and provides
 * detailed context for compliance reporting and incident investigation.
 */
@Entity
@Table(name = "claim_audit_log", indexes = {
    @Index(name = "idx_audit_claim_id", columnList = "claim_id"),
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_performed_at", columnList = "performed_at"),
    @Index(name = "idx_audit_performed_by", columnList = "performed_by"),
    @Index(name = "idx_audit_correlation_id", columnList = "correlation_id")
})
public class ClaimAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuditEventType eventType;

    // State transition specific fields
    @Enumerated(EnumType.STRING)
    @Column(name = "from_state")
    private ClaimState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state")
    private ClaimState toState;

    // Event details and context
    @Column(name = "event_description", nullable = false, length = 1000)
    private String eventDescription;

    @Column(name = "event_details", columnDefinition = "TEXT")
    private String eventDetails; // JSON payload with full context

    @Column(name = "business_reason", length = 500)
    private String businessReason;

    // User and session information
    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "session_id")
    private String sessionId;

    // Event correlation and tracing
    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "event_id")
    private String eventId; // Link to domain event

    // Risk and compliance fields
    @Column(name = "risk_level")
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(name = "compliance_flags")
    private String complianceFlags; // JSON array of compliance requirements met

    @Column(name = "regulatory_notes", columnDefinition = "TEXT")
    private String regulatoryNotes;

    // System and technical information
    @Column(name = "system_component")
    private String systemComponent; // Which part of system generated event

    @Column(name = "api_version")
    private String apiVersion;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    // Data before and after changes
    @Column(name = "data_before", columnDefinition = "TEXT")
    private String dataBefore; // JSON snapshot of data before change

    @Column(name = "data_after", columnDefinition = "TEXT")
    private String dataAfter; // JSON snapshot of data after change

    // External system integration
    @Column(name = "external_system_ref")
    private String externalSystemRef;

    @Column(name = "external_event_id")
    private String externalEventId;

    // Default constructor for JPA
    protected ClaimAuditLog() {}

    /**
     * Constructor for creating audit log entries
     */
    private ClaimAuditLog(Builder builder) {
        this.claimId = builder.claimId;
        this.eventType = builder.eventType;
        this.fromState = builder.fromState;
        this.toState = builder.toState;
        this.eventDescription = builder.eventDescription;
        this.eventDetails = builder.eventDetails;
        this.businessReason = builder.businessReason;
        this.performedBy = builder.performedBy;
        this.performedAt = builder.performedAt != null ? builder.performedAt : LocalDateTime.now();
        this.ipAddress = builder.ipAddress;
        this.userAgent = builder.userAgent;
        this.sessionId = builder.sessionId;
        this.correlationId = builder.correlationId;
        this.eventId = builder.eventId;
        this.riskLevel = builder.riskLevel;
        this.complianceFlags = builder.complianceFlags;
        this.regulatoryNotes = builder.regulatoryNotes;
        this.systemComponent = builder.systemComponent;
        this.apiVersion = builder.apiVersion;
        this.processingTimeMs = builder.processingTimeMs;
        this.dataBefore = builder.dataBefore;
        this.dataAfter = builder.dataAfter;
        this.externalSystemRef = builder.externalSystemRef;
        this.externalEventId = builder.externalEventId;
    }

    /**
     * Create audit log for state transition
     */
    public static ClaimAuditLog forStateTransition(Long claimId, ClaimState fromState, ClaimState toState,
                                                  String reason, String performedBy, String correlationId) {
        return new Builder(claimId, AuditEventType.STATE_TRANSITION)
            .fromState(fromState)
            .toState(toState)
            .eventDescription(String.format("State transition from %s to %s", fromState, toState))
            .businessReason(reason)
            .performedBy(performedBy)
            .correlationId(correlationId)
            .build();
    }

    /**
     * Create audit log for assignment change
     */
    public static ClaimAuditLog forAssignmentChange(Long claimId, String assigneeType, String assignee,
                                                   String assignedBy, String correlationId) {
        return new Builder(claimId, AuditEventType.ASSIGNMENT_CHANGE)
            .eventDescription(String.format("Assigned to %s (%s)", assignee, assigneeType))
            .performedBy(assignedBy)
            .correlationId(correlationId)
            .build();
    }

    /**
     * Create audit log for fraud score update
     */
    public static ClaimAuditLog forFraudScoreUpdate(Long claimId, Double oldScore, Double newScore,
                                                   String performedBy, String correlationId) {
        return new Builder(claimId, AuditEventType.FRAUD_SCORE_UPDATE)
            .eventDescription(String.format("Fraud score updated from %.2f to %.2f", oldScore, newScore))
            .performedBy(performedBy)
            .correlationId(correlationId)
            .riskLevel(determineRiskLevel(newScore))
            .build();
    }

    /**
     * Determine risk level from fraud score
     */
    private static RiskLevel determineRiskLevel(Double score) {
        if (score == null) return RiskLevel.LOW;
        if (score >= 70.0) return RiskLevel.CRITICAL;
        if (score >= 50.0) return RiskLevel.HIGH;
        if (score >= 25.0) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
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

    public String getEventDescription() {
        return eventDescription;
    }

    public String getEventDetails() {
        return eventDetails;
    }

    public String getBusinessReason() {
        return businessReason;
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

    public String getCorrelationId() {
        return correlationId;
    }

    public String getEventId() {
        return eventId;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getComplianceFlags() {
        return complianceFlags;
    }

    public String getRegulatoryNotes() {
        return regulatoryNotes;
    }

    public String getSystemComponent() {
        return systemComponent;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public String getDataBefore() {
        return dataBefore;
    }

    public String getDataAfter() {
        return dataAfter;
    }

    public String getExternalSystemRef() {
        return externalSystemRef;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    @Override
    public String toString() {
        return String.format("ClaimAuditLog{id=%d, claimId=%d, type=%s, by=%s, at=%s}",
                           id, claimId, eventType, performedBy, performedAt);
    }

    /**
     * Builder pattern for creating audit log entries
     */
    public static class Builder {
        private final Long claimId;
        private final AuditEventType eventType;

        private ClaimState fromState;
        private ClaimState toState;
        private String eventDescription;
        private String eventDetails;
        private String businessReason;
        private String performedBy;
        private LocalDateTime performedAt;
        private String ipAddress;
        private String userAgent;
        private String sessionId;
        private String correlationId;
        private String eventId;
        private RiskLevel riskLevel;
        private String complianceFlags;
        private String regulatoryNotes;
        private String systemComponent;
        private String apiVersion;
        private Long processingTimeMs;
        private String dataBefore;
        private String dataAfter;
        private String externalSystemRef;
        private String externalEventId;

        public Builder(Long claimId, AuditEventType eventType) {
            this.claimId = claimId;
            this.eventType = eventType;
        }

        public Builder fromState(ClaimState fromState) {
            this.fromState = fromState;
            return this;
        }

        public Builder toState(ClaimState toState) {
            this.toState = toState;
            return this;
        }

        public Builder eventDescription(String eventDescription) {
            this.eventDescription = eventDescription;
            return this;
        }

        public Builder eventDetails(String eventDetails) {
            this.eventDetails = eventDetails;
            return this;
        }

        public Builder businessReason(String businessReason) {
            this.businessReason = businessReason;
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

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder riskLevel(RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
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

        public Builder systemComponent(String systemComponent) {
            this.systemComponent = systemComponent;
            return this;
        }

        public Builder apiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public Builder processingTimeMs(Long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public Builder dataBefore(String dataBefore) {
            this.dataBefore = dataBefore;
            return this;
        }

        public Builder dataAfter(String dataAfter) {
            this.dataAfter = dataAfter;
            return this;
        }

        public Builder externalSystemRef(String externalSystemRef) {
            this.externalSystemRef = externalSystemRef;
            return this;
        }

        public Builder externalEventId(String externalEventId) {
            this.externalEventId = externalEventId;
            return this;
        }

        public ClaimAuditLog build() {
            return new ClaimAuditLog(this);
        }
    }

    /**
     * Enumeration for different types of audit events
     */
    public enum AuditEventType {
        // State and workflow events
        STATE_TRANSITION("State transition"),
        ASSIGNMENT_CHANGE("Assignment change"),
        WORKFLOW_ACTION("Workflow action"),

        // Fraud detection events
        FRAUD_SCORE_UPDATE("Fraud score update"),
        FRAUD_ANALYSIS_TRIGGERED("Fraud analysis triggered"),
        SIU_ESCALATION("SIU escalation"),

        // Investigation events
        INVESTIGATION_ACTION("Investigation action"),
        EVIDENCE_ADDED("Evidence added"),
        INVESTIGATION_NOTE("Investigation note"),

        // System events
        SYSTEM_ACTION("System action"),
        API_ACCESS("API access"),
        DOCUMENT_UPLOAD("Document upload"),
        DOCUMENT_ACCESS("Document access"),

        // External integration events
        EXTERNAL_SYSTEM_SYNC("External system sync"),
        NOTIFICATION_SENT("Notification sent"),

        // Security and compliance events
        SECURITY_EVENT("Security event"),
        COMPLIANCE_CHECK("Compliance check"),
        MANUAL_OVERRIDE("Manual override"),

        // Data events
        DATA_EXPORT("Data export"),
        DATA_CORRECTION("Data correction");

        private final String description;

        AuditEventType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Risk level enumeration for audit categorization
     */
    public enum RiskLevel {
        LOW("Low risk"),
        MEDIUM("Medium risk"),
        HIGH("High risk"),
        CRITICAL("Critical risk");

        private final String description;

        RiskLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Add a static builder method for the Builder pattern
    public static Builder builder(Long claimId, AuditEventType eventType) {
        return new Builder(claimId, eventType);
    }
}