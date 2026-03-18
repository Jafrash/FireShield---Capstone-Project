package org.hartford.fireinsurance.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "documents")
public class Document {

    // Nested ENUM for Document Type
    public enum DocumentType {
        // Customer Documents (Policy Stage)
        PROPOSAL_FORM,
        KYC_AADHAR,
        KYC_PAN,
        KYC_GST,
        ADDRESS_PROOF,
        OWNERSHIP_PROOF,
        RENT_AGREEMENT,
        LEASE_DEED,
        PREVIOUS_POLICY,

        // Property Documents
        BUILDING_PLAN,
        CONSTRUCTION_DETAILS,
        ELECTRICAL_CERTIFICATE,
        FIRE_SAFETY_CERTIFICATE,
        NOC_FIRE_DEPARTMENT,
        MACHINERY_LIST,
        STOCK_STATEMENT,

        // Financial Documents
        FINANCIAL_STATEMENT,
        GST_RETURN,
        STOCK_REGISTER,
        BANK_STATEMENT,

        // Surveyor Documents (Property Inspection)
        RISK_INSPECTION_REPORT,
        SITE_PHOTOS,
        FIRE_PROTECTION_PHOTOS,
        ELECTRICAL_PANEL_PHOTOS,
        HAZARD_ASSESSMENT,
        RECOMMENDATION_REPORT,
        RISK_GRADING_SHEET,

        // Customer Documents (Claim Stage)
        CLAIM_FORM,
        FIRE_BRIGADE_REPORT,
        FIR_COPY,
        LOSS_ESTIMATE,
        DAMAGE_PHOTOS,
        REPAIR_QUOTATION,
        PURCHASE_INVOICE,
        SALES_INVOICE,
        ASSET_REGISTER,

        // Surveyor Documents (Claim Stage)
        SPOT_SURVEY_REPORT,
        FINAL_SURVEY_REPORT,
        CAUSE_OF_LOSS_ANALYSIS,
        LOSS_ASSESSMENT_SHEET,
        DEPRECIATION_CALCULATION,
        SALVAGE_VALUE_ASSESSMENT,
        SETTLEMENT_RECOMMENDATION,
        UNDERINSURANCE_CALCULATION,

        // Legal Documents
        SUBROGATION_LETTER,
        INDEMNITY_BOND,
        DISCHARGE_VOUCHER,
        NOC_BANK,

        // Other
        OTHER
    }

    // Nested ENUM for Document Stage
    public enum DocumentStage {
        POLICY_STAGE,      // During policy issuance
        INSPECTION_STAGE,  // During property inspection
        CLAIM_STAGE        // During claim processing
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surveyor_id")
    private Surveyor surveyor;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_stage")
    private DocumentStage documentStage;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Column(name = "uploaded_by_username")
    private String uploadedBy;

    // Constructors
    public Document() {
    }

    public Document(Long documentId, Claim claim, Property property, Customer customer, Surveyor surveyor,
                    String fileName, String filePath, DocumentType documentType, DocumentStage documentStage,
                    Long fileSize, String contentType, LocalDateTime uploadDate, String uploadedBy) {
        this.documentId = documentId;
        this.claim = claim;
        this.property = property;
        this.customer = customer;
        this.surveyor = surveyor;
        this.fileName = fileName;
        this.filePath = filePath;
        this.documentType = documentType;
        this.documentStage = documentStage;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadDate = uploadDate;
        this.uploadedBy = uploadedBy;
    }

    // Getters and Setters
    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Surveyor getSurveyor() {
        return surveyor;
    }

    public void setSurveyor(Surveyor surveyor) {
        this.surveyor = surveyor;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
