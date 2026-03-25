package org.hartford.fireinsurance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing structured investigation notes.
 * Provides rich text note capabilities with proper metadata tracking.
 */
@Entity
@Table(name = "investigation_notes")
public class InvestigationNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigation_case_id", nullable = false)
    private InvestigationCase investigationCase;

    @Column(name = "note_title")
    private String noteTitle;

    @Column(name = "note_content", columnDefinition = "TEXT", nullable = false)
    private String noteContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type")
    private NoteType noteType = NoteType.GENERAL;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_important")
    private Boolean isImportant = false;

    @Column(name = "is_confidential")
    private Boolean isConfidential = false;

    // Note Type Enum
    public enum NoteType {
        GENERAL,
        INTERVIEW,
        OBSERVATION,
        EVIDENCE_ANALYSIS,
        FOLLOW_UP,
        CONCLUSION,
        RECOMMENDATION
    }

    // Constructors
    public InvestigationNote() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public InvestigationNote(InvestigationCase investigationCase, String noteContent, String createdBy) {
        this();
        this.investigationCase = investigationCase;
        this.noteContent = noteContent;
        this.createdBy = createdBy;
    }

    public InvestigationNote(InvestigationCase investigationCase, String noteTitle, String noteContent,
                           NoteType noteType, String createdBy) {
        this();
        this.investigationCase = investigationCase;
        this.noteTitle = noteTitle;
        this.noteContent = noteContent;
        this.noteType = noteType;
        this.createdBy = createdBy;
    }

    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public String getFormattedNoteType() {
        return noteType != null ? noteType.name().replace("_", " ") : "General";
    }

    public boolean isRecent() {
        return createdAt != null && createdAt.isAfter(LocalDateTime.now().minusHours(24));
    }

    // Getters and Setters
    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public InvestigationCase getInvestigationCase() {
        return investigationCase;
    }

    public void setInvestigationCase(InvestigationCase investigationCase) {
        this.investigationCase = investigationCase;
    }

    public String getNoteTitle() {
        return noteTitle;
    }

    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }

    public NoteType getNoteType() {
        return noteType;
    }

    public void setNoteType(NoteType noteType) {
        this.noteType = noteType;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsImportant() {
        return isImportant;
    }

    public void setIsImportant(Boolean isImportant) {
        this.isImportant = isImportant;
    }

    public Boolean getIsConfidential() {
        return isConfidential;
    }

    public void setIsConfidential(Boolean isConfidential) {
        this.isConfidential = isConfidential;
    }

    @Override
    public String toString() {
        return "InvestigationNote{" +
                "noteId=" + noteId +
                ", noteTitle='" + noteTitle + '\'' +
                ", noteType=" + noteType +
                ", createdBy='" + createdBy + '\'' +
                ", createdAt=" + createdAt +
                ", isImportant=" + isImportant +
                '}';
    }
}