package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.Document.DocumentType;
import org.hartford.fireinsurance.model.Document.DocumentStage;
import java.time.LocalDateTime;

public class DocumentResponse {
    private Long documentId;
    private String fileName;
    private DocumentType documentType;
    private DocumentStage documentStage;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadDate;
    private String uploadedBy;

    public DocumentResponse() {}

    public DocumentResponse(Long documentId, String fileName, DocumentType documentType,
                           DocumentStage documentStage, Long fileSize, String contentType,
                           LocalDateTime uploadDate, String uploadedBy) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.documentType = documentType;
        this.documentStage = documentStage;
        this.fileSize = fileSize;
        this.contentType = contentType;
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

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
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

