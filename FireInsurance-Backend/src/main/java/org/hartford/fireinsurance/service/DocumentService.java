package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.model.*;
import org.hartford.fireinsurance.model.Document.DocumentStage;
import org.hartford.fireinsurance.model.Document.DocumentType;
import org.hartford.fireinsurance.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final CustomerService customerService;
    private final SurveyorService surveyorService;
    private final ClaimService claimService;
    private final PropertyService propertyService;

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    public DocumentService(DocumentRepository documentRepository,
                          CustomerService customerService,
                          SurveyorService surveyorService,
                          ClaimService claimService,
                          PropertyService propertyService) {
        this.documentRepository = documentRepository;
        this.customerService = customerService;
        this.surveyorService = surveyorService;
        this.claimService = claimService;
        this.propertyService = propertyService;
    }

    /**
     * Upload document for customer
     */
    public Document uploadCustomerDocument(String username, MultipartFile file,
                                          DocumentType documentType, DocumentStage documentStage,
                                          Long propertyId, Long claimId) throws IOException {
        log.info("📁 DocumentService.uploadCustomerDocument called");
        log.info("   Username: {}", username);
        log.info("   File: {}", file.getOriginalFilename());

        Customer customer;
        try {
            customer = customerService.getCustomerByUsername(username);
            log.info("✅ Customer found: ID = {}", customer.getCustomerId());
        } catch (Exception e) {
            log.error("❌ Customer not found for username: {}", username);
            throw new RuntimeException("Customer not found for username: " + username, e);
        }

        // Save file
        log.info("💾 Saving file to filesystem...");
        String fileName;
        try {
            fileName = saveFile(file);
            log.info("✅ File saved: {}", fileName);
        } catch (IOException e) {
            log.error("❌ Failed to save file: {}", e.getMessage());
            throw e;
        }

        // Create document entity
        log.info("📄 Creating document entity...");
        Document document = new Document();
        document.setCustomer(customer);
        document.setFileName(file.getOriginalFilename());
        document.setFilePath(fileName);
        document.setDocumentType(documentType);
        document.setDocumentStage(documentStage);
        document.setFileSize(file.getSize());
        document.setContentType(file.getContentType());
        document.setUploadDate(LocalDateTime.now());
        document.setUploadedBy(username);

        // Link to property or claim if provided
        if (propertyId != null) {
            log.info("🏠 Linking to property ID: {}", propertyId);
            try {
                Property property = propertyService.getPropertyById(propertyId);
                // Verify ownership
                if (!property.getCustomer().getUser().getUsername().equals(username)) {
                    log.error("❌ Unauthorized: Property does not belong to customer");
                    throw new RuntimeException("Unauthorized: Property does not belong to this customer");
                }
                document.setProperty(property);
                log.info("✅ Property linked successfully");
            } catch (Exception e) {
                log.error("❌ Failed to link property: {}", e.getMessage());
                throw e;
            }
        }

        if (claimId != null) {
            log.info("📋 Linking to claim ID: {}", claimId);
            try {
                Claim claim = claimService.getClaimById(claimId);
                // Verify ownership
                if (!claim.getSubscription().getCustomer().getUser().getUsername().equals(username)) {
                    log.error("❌ Unauthorized: Claim does not belong to customer");
                    throw new RuntimeException("Unauthorized: Claim does not belong to this customer");
                }
                document.setClaim(claim);
                log.info("✅ Claim linked successfully");
            } catch (Exception e) {
                log.error("❌ Failed to link claim: {}", e.getMessage());
                throw e;
            }
        }

        log.info("💾 Saving document to database...");
        try {
            Document savedDocument = documentRepository.save(document);
            log.info("✅ Document saved to database. Document ID: {}", savedDocument.getDocumentId());
            return savedDocument;
        } catch (Exception e) {
            log.error("❌ Failed to save document to database: {}", e.getMessage());
            throw new RuntimeException("Failed to save document to database", e);
        }
    }

    /**
     * Upload document for surveyor
     */
    public Document uploadSurveyorDocument(String username, MultipartFile file,
                                          DocumentType documentType, DocumentStage documentStage,
                                          Long claimId) throws IOException {
        Surveyor surveyor = surveyorService.getSurveyorByUsername(username);

        // Save file
        String fileName = saveFile(file);

        // Create document entity
        Document document = new Document();
        document.setSurveyor(surveyor);
        document.setFileName(file.getOriginalFilename());
        document.setFilePath(fileName);
        document.setDocumentType(documentType);
        document.setDocumentStage(documentStage);
        document.setFileSize(file.getSize());
        document.setContentType(file.getContentType());
        document.setUploadDate(LocalDateTime.now());
        document.setUploadedBy(username);

        // Link to claim
        if (claimId != null) {
            Claim claim = claimService.getClaimById(claimId);
            document.setClaim(claim);
        }

        return documentRepository.save(document);
    }

    /**
     * Save file to filesystem
     */
    private String saveFile(MultipartFile file) throws IOException {
        log.info("💾 Saving file to filesystem");
        log.info("   Upload directory: {}", uploadDir);

        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            log.info("📁 Upload directory does not exist. Creating...");
            try {
                Files.createDirectories(uploadPath);
                log.info("✅ Upload directory created: {}", uploadPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("❌ Failed to create upload directory: {}", e.getMessage());
                throw new IOException("Failed to create upload directory: " + uploadPath.toAbsolutePath(), e);
            }
        } else {
            log.info("✅ Upload directory exists: {}", uploadPath.toAbsolutePath());
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : "";
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        log.info("📝 Generated unique filename: {}", uniqueFileName);

        // Save file
        Path filePath = uploadPath.resolve(uniqueFileName);
        log.info("💾 Saving file to: {}", filePath.toAbsolutePath());

        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ File saved successfully");

            // Verify file was saved
            if (Files.exists(filePath)) {
                long fileSize = Files.size(filePath);
                log.info("✅ File verified. Size: {} bytes", fileSize);
            } else {
                log.error("❌ File was not saved properly");
                throw new IOException("File was not saved properly");
            }
        } catch (IOException e) {
            log.error("❌ Failed to save file: {}", e.getMessage());
            throw e;
        }

        return uniqueFileName;
    }

    /**
     * Load file as resource
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException("File not found: " + fileName, e);
        }
    }

    /**
     * Get documents by customer
     */
    public List<Document> getDocumentsByCustomer(String username) {
        Customer customer = customerService.getCustomerByUsername(username);
        return documentRepository.findByCustomer(customer);
    }

    /**
     * Get documents by surveyor
     */
    public List<Document> getDocumentsBySurveyor(String username) {
        Surveyor surveyor = surveyorService.getSurveyorByUsername(username);
        return documentRepository.findBySurveyor(surveyor);
    }

    /**
     * Get documents by claim
     */
    public List<Document> getDocumentsByClaim(Long claimId) {
        Claim claim = claimService.getClaimById(claimId);
        return documentRepository.findByClaim(claim);
    }

    /**
     * Get documents by property
     */
    public List<Document> getDocumentsByProperty(Long propertyId) {
        Property property = propertyService.getPropertyById(propertyId);
        return documentRepository.findByProperty(property);
    }

    /**
     * Get all documents (Admin only)
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    /**
     * Get document by ID
     */
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + id));
    }

    /**
     * Delete document
     */
    public void deleteDocument(Long id, String username) {
        Document document = getDocumentById(id);

        // Security check: only uploader or admin can delete
        if (!document.getUploadedBy().equals(username)) {
            throw new RuntimeException("Unauthorized: You can only delete your own documents");
        }

        // Delete file from filesystem
        try {
            Path filePath = Paths.get(uploadDir).resolve(document.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but continue with database deletion
            e.printStackTrace();
        }

        documentRepository.delete(document);
    }

    // Old method for backward compatibility
    public Document save(Document document) {
        return documentRepository.save(document);
    }
}
