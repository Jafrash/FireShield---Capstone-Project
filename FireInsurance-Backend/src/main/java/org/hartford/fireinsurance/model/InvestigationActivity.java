package org.hartford.fireinsurance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing investigation timeline activities.
 * Provides automated timeline generation and activity tracking for investigations.
 */
@Entity
@Table(name = "investigation_activities")
public class InvestigationActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigation_case_id", nullable = false)
    private InvestigationCase investigationCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Column(name = "activity_description", columnDefinition = "TEXT", nullable = false)
    private String activityDescription;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(name = "additional_details", columnDefinition = "TEXT")
    private String additionalDetails;

    // Reference to related entities (optional)
    @Column(name = "related_evidence_id")
    private Long relatedEvidenceId;

    @Column(name = "related_note_id")
    private Long relatedNoteId;

    @Column(name = "system_generated")
    private Boolean systemGenerated = false;

    // Activity Type Enum
    public enum ActivityType {
        CASE_ASSIGNED,
        INVESTIGATION_STARTED,
        EVIDENCE_UPLOADED,
        NOTE_ADDED,
        STATUS_CHANGED,
        INTERVIEW_CONDUCTED,
        DOCUMENT_REVIEWED,
        SITE_VISIT,
        REPORT_GENERATED,
        CASE_COMPLETED,
        CUSTOM
    }

    // Constructors
    public InvestigationActivity() {
        this.performedAt = LocalDateTime.now();
    }

    public InvestigationActivity(InvestigationCase investigationCase, ActivityType activityType,
                               String activityDescription, String performedBy) {
        this();
        this.investigationCase = investigationCase;
        this.activityType = activityType;
        this.activityDescription = activityDescription;
        this.performedBy = performedBy;
    }

    public InvestigationActivity(InvestigationCase investigationCase, ActivityType activityType,
                               String activityDescription, String performedBy, boolean systemGenerated) {
        this();
        this.investigationCase = investigationCase;
        this.activityType = activityType;
        this.activityDescription = activityDescription;
        this.performedBy = performedBy;
        this.systemGenerated = systemGenerated;
    }

    // Helper methods
    public String getFormattedActivityType() {
        return activityType != null ? activityType.name().replace("_", " ") : "Unknown";
    }

    public boolean isRecent() {
        return performedAt != null && performedAt.isAfter(LocalDateTime.now().minusHours(24));
    }

    public String getActivityIcon() {
        if (activityType == null) return "event";

        switch (activityType) {
            case CASE_ASSIGNED: return "assignment";
            case INVESTIGATION_STARTED: return "play_arrow";
            case EVIDENCE_UPLOADED: return "attach_file";
            case NOTE_ADDED: return "note_add";
            case STATUS_CHANGED: return "update";
            case INTERVIEW_CONDUCTED: return "record_voice_over";
            case DOCUMENT_REVIEWED: return "description";
            case SITE_VISIT: return "location_on";
            case REPORT_GENERATED: return "assignment_turned_in";
            case CASE_COMPLETED: return "check_circle";
            default: return "event";
        }
    }

    // Factory methods for common activities
    public static InvestigationActivity createCaseAssignedActivity(InvestigationCase investigationCase,
                                                                  String investigatorName,
                                                                  String assignedBy) {
        String description = String.format("Case assigned to investigator %s", investigatorName);
        return new InvestigationActivity(investigationCase, ActivityType.CASE_ASSIGNED,
                                       description, assignedBy, true);
    }

    public static InvestigationActivity createInvestigationStartedActivity(InvestigationCase investigationCase,
                                                                         String investigatorName) {
        String description = String.format("Investigation started by %s", investigatorName);
        return new InvestigationActivity(investigationCase, ActivityType.INVESTIGATION_STARTED,
                                       description, investigatorName, true);
    }

    public static InvestigationActivity createEvidenceUploadedActivity(InvestigationCase investigationCase,
                                                                     String fileName,
                                                                     String uploadedBy) {
        String description = String.format("Evidence file '%s' uploaded", fileName);
        return new InvestigationActivity(investigationCase, ActivityType.EVIDENCE_UPLOADED,
                                       description, uploadedBy, true);
    }

    public static InvestigationActivity createNoteAddedActivity(InvestigationCase investigationCase,
                                                              String noteTitle,
                                                              String addedBy) {
        String description = String.format("Investigation note added: %s",
                                         noteTitle != null ? noteTitle : "Untitled");
        return new InvestigationActivity(investigationCase, ActivityType.NOTE_ADDED,
                                       description, addedBy, true);
    }

    public static InvestigationActivity createStatusChangedActivity(InvestigationCase investigationCase,
                                                                  String oldStatus,
                                                                  String newStatus,
                                                                  String changedBy) {
        String description = String.format("Status changed from %s to %s", oldStatus, newStatus);
        return new InvestigationActivity(investigationCase, ActivityType.STATUS_CHANGED,
                                       description, changedBy, true);
    }

    // Getters and Setters
    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public InvestigationCase getInvestigationCase() {
        return investigationCase;
    }

    public void setInvestigationCase(InvestigationCase investigationCase) {
        this.investigationCase = investigationCase;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public String getActivityDescription() {
        return activityDescription;
    }

    public void setActivityDescription(String activityDescription) {
        this.activityDescription = activityDescription;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(LocalDateTime performedAt) {
        this.performedAt = performedAt;
    }

    public String getAdditionalDetails() {
        return additionalDetails;
    }

    public void setAdditionalDetails(String additionalDetails) {
        this.additionalDetails = additionalDetails;
    }

    public Long getRelatedEvidenceId() {
        return relatedEvidenceId;
    }

    public void setRelatedEvidenceId(Long relatedEvidenceId) {
        this.relatedEvidenceId = relatedEvidenceId;
    }

    public Long getRelatedNoteId() {
        return relatedNoteId;
    }

    public void setRelatedNoteId(Long relatedNoteId) {
        this.relatedNoteId = relatedNoteId;
    }

    public Boolean getSystemGenerated() {
        return systemGenerated;
    }

    public void setSystemGenerated(Boolean systemGenerated) {
        this.systemGenerated = systemGenerated;
    }

    @Override
    public String toString() {
        return "InvestigationActivity{" +
                "activityId=" + activityId +
                ", activityType=" + activityType +
                ", activityDescription='" + activityDescription + '\'' +
                ", performedBy='" + performedBy + '\'' +
                ", performedAt=" + performedAt +
                ", systemGenerated=" + systemGenerated +
                '}';
    }
}