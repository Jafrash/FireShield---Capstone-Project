package org.hartford.fireinsurance.dto;
import org.hartford.fireinsurance.model.Document.DocumentType;
public class UploadDocumentRequest {
    private Long propertyId;   // optional
    private Long claimId;      // optional
    private DocumentType documentType;
    private String fileName;
    private String fileType;
    private String filePath;
    public UploadDocumentRequest() {}
    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }
    public Long getClaimId() { return claimId; }
    public void setClaimId(Long claimId) { this.claimId = claimId; }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
