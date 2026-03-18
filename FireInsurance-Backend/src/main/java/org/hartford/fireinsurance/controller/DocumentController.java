package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.DocumentResponse;
import org.hartford.fireinsurance.model.Document;
import org.hartford.fireinsurance.model.Document.DocumentStage;
import org.hartford.fireinsurance.model.Document.DocumentType;
import org.hartford.fireinsurance.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;



@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<DocumentResponse> uploadCustomerDocument(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("documentStage") DocumentStage documentStage,
            @RequestParam(value = "propertyId", required = false) Long propertyId,
            @RequestParam(value = "claimId", required = false) Long claimId) {
        try {
            String username = authentication.getName();
            log.info("🔵 Customer document upload started");
            log.info("   Username: {}", username);
            log.info("   Filename: {}", file.getOriginalFilename());
            log.info("   File size: {} bytes", file.getSize());
            log.info("   Content type: {}", file.getContentType());
            log.info("   Document type: {}", documentType);
            log.info("   Document stage: {}", documentStage);
            log.info("   Property ID: {}", propertyId);
            log.info("   Claim ID: {}", claimId);

            if (file.isEmpty()) {
                log.error("❌ File is empty");
                throw new RuntimeException("File is empty");
            }

            Document document = documentService.uploadCustomerDocument(
                    username, file, documentType, documentStage, propertyId, claimId);

            log.info("✅ Document uploaded successfully. Document ID: {}", document.getDocumentId());
            return ResponseEntity.ok(mapToResponse(document));
        } catch (Exception e) {
            log.error("❌ Failed to upload document: {}", e.getMessage());
            log.error("Stack trace:", e);
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed: " + e.getMessage(), e);
        }
    }

    @PostMapping("/upload/surveyor")
    @PreAuthorize("hasRole('SURVEYOR')")
    public ResponseEntity<DocumentResponse> uploadSurveyorDocument(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("documentStage") DocumentStage documentStage,
            @RequestParam(value = "claimId", required = false) Long claimId) {
        try {
            String username = authentication.getName();
            log.info("🔵 Surveyor document upload started");
            log.info("   Username: {}", username);
            log.info("   Filename: {}", file.getOriginalFilename());
            log.info("   File size: {} bytes", file.getSize());
            log.info("   Document type: {}", documentType);
            log.info("   Document stage: {}", documentStage);
            log.info("   Claim ID: {}", claimId);

            if (file.isEmpty()) {
                log.error("❌ File is empty");
                throw new RuntimeException("File is empty");
            }

            Document document = documentService.uploadSurveyorDocument(
                    username, file, documentType, documentStage, claimId);

            log.info("✅ Document uploaded successfully. Document ID: {}", document.getDocumentId());
            return ResponseEntity.ok(mapToResponse(document));
        } catch (Exception e) {
            log.error("❌ Failed to upload document: {}", e.getMessage());
            log.error("Stack trace:", e);
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed: " + e.getMessage(), e);
        }
    }

    @GetMapping("/download/{documentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','SURVEYOR','ADMIN', 'UNDERWRITER')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        Document document = documentService.getDocumentById(documentId);
        Resource resource = documentService.loadFileAsResource(document.getFilePath());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getFileName() + "\"")
                .body(resource);
    }

    // Alias for backward compatibility - /me redirects based on role
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER','SURVEYOR')")
    public ResponseEntity<List<DocumentResponse>> getMyDocumentsAlias(Authentication authentication) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        List<DocumentResponse> response;
        if (role.equals("ROLE_SURVEYOR")) {
            response = documentService.getDocumentsBySurveyor(username)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } else {
            response = documentService.getDocumentsByCustomer(username)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(Authentication authentication) {
        String username = authentication.getName();
        List<DocumentResponse> response = documentService.getDocumentsByCustomer(username)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/surveyor/me")
    @PreAuthorize("hasRole('SURVEYOR')")
    public ResponseEntity<List<DocumentResponse>> getMySurveyorDocuments(Authentication authentication) {
        String username = authentication.getName();
        List<DocumentResponse> response = documentService.getDocumentsBySurveyor(username)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/claim/{claimId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','SURVEYOR','ADMIN', 'UNDERWRITER')")
    public ResponseEntity<List<DocumentResponse>> getClaimDocuments(@PathVariable Long claimId) {
        List<DocumentResponse> response = documentService.getDocumentsByClaim(claimId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN', 'UNDERWRITER')")
    public ResponseEntity<List<DocumentResponse>> getPropertyDocuments(@PathVariable Long propertyId) {
        List<DocumentResponse> response = documentService.getDocumentsByProperty(propertyId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        List<DocumentResponse> response = documentService.getAllDocuments()
                .stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','SURVEYOR','ADMIN')")
    public ResponseEntity<String> deleteDocument(
            Authentication authentication,
            @PathVariable Long documentId) {
        String username = authentication.getName();
        documentService.deleteDocument(documentId, username);
        return ResponseEntity.ok("Document deleted successfully");
    }

    private DocumentResponse mapToResponse(Document document) {
        return new DocumentResponse(
                document.getDocumentId(),
                document.getFileName(),
                document.getDocumentType(),
                document.getDocumentStage(),
                document.getFileSize(),
                document.getContentType(),
                document.getUploadDate(),
                document.getUploadedBy()
        );
    }
}
