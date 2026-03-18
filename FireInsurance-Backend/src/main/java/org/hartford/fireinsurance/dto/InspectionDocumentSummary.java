package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.Document.DocumentStage;
import org.hartford.fireinsurance.model.Document.DocumentType;

import java.time.LocalDateTime;

public class InspectionDocumentSummary {
    private Long documentId;
    private String fileName;
    private DocumentType documentType;
    private DocumentStage documentStage;
    private LocalDateTime uploadDate;
    private String uploadedBy;

    public InspectionDocumentSummary() {
    }

    public InspectionDocumentSummary(Long documentId, String fileName, DocumentType documentType,
                                     DocumentStage documentStage, LocalDateTime uploadDate, String uploadedBy) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.documentType = documentType;
        this.documentStage = documentStage;
        this.uploadDate = uploadDate;
        this.uploadedBy = uploadedBy;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public DocumentStage getDocumentStage() {
        return documentStage;
    }

    public void setDocumentStage(DocumentStage documentStage) {
        this.documentStage = documentStage;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
