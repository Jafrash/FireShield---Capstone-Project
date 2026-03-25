package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.model.Evidence;
import org.hartford.fireinsurance.model.InvestigationCase;
import org.hartford.fireinsurance.model.InvestigationActivity;
import org.hartford.fireinsurance.repository.EvidenceRepository;
import org.hartford.fireinsurance.repository.InvestigationCaseRepository;
import org.hartford.fireinsurance.repository.InvestigationActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Set;
import java.util.UUID;

/**
 * Service class for managing evidence files in SIU investigations.
 * Handles file upload, storage, metadata management, and evidence retrieval.
 */
@Service
@Transactional
public class EvidenceService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceService.class);

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private InvestigationCaseRepository investigationCaseRepository;

    @Autowired
    private InvestigationActivityRepository investigationActivityRepository;

    @Value("${app.evidence.upload.dir:uploads/evidence}")
    private String evidenceUploadDir;

    @Value("${app.evidence.max.file.size:10485760}") // 10MB default
    private Long maxFileSize;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf", "text/plain", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/avi", "video/mov", "video/wmv"
    );

    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/mp3", "audio/wav", "audio/m4a", "audio/aac"
    );

    /**
     * Upload evidence file for an investigation case.
     */
    public Evidence uploadEvidence(Long investigationCaseId, MultipartFile file, String description,
                                 Set<String> tags, Boolean isCritical, String uploadedBy) throws IOException {
        log.info("Uploading evidence for investigation case ID: {}, file: {}", investigationCaseId, file.getOriginalFilename());

        // Validate input
        validateFileUpload(file);

        InvestigationCase investigationCase = investigationCaseRepository.findById(investigationCaseId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationCaseId));

        // Determine evidence type based on MIME type
        Evidence.EvidenceType evidenceType = determineEvidenceType(file.getContentType());

        // Generate unique file name
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String fileName = UUID.randomUUID().toString() + fileExtension;

        // Create storage path
        Path uploadPath = createUploadPath(investigationCaseId);
        Path filePath = uploadPath.resolve(fileName);

        // Save file to storage
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create evidence entity
        Evidence evidence = new Evidence(investigationCase, fileName, originalFileName, evidenceType,
                                       filePath.toString(), uploadedBy);
        evidence.setDescription(description);
        evidence.setFileSize(file.getSize());
        evidence.setMimeType(file.getContentType());
        evidence.setIsCritical(isCritical != null ? isCritical : false);

        if (tags != null && !tags.isEmpty()) {
            evidence.setTags(tags);
        }

        // Save evidence to database
        Evidence savedEvidence = evidenceRepository.save(evidence);

        // Create activity record
        InvestigationActivity activity = InvestigationActivity.createEvidenceUploadedActivity(
                investigationCase, originalFileName, uploadedBy);
        activity.setRelatedEvidenceId(savedEvidence.getEvidenceId());
        investigationActivityRepository.save(activity);

        log.info("Successfully uploaded evidence ID: {} for investigation case ID: {}",
                savedEvidence.getEvidenceId(), investigationCaseId);

        return savedEvidence;
    }

    /**
     * Get all evidence for an investigation case.
     */
    public List<Evidence> getEvidenceByCase(Long investigationCaseId) {
        log.info("Fetching evidence for investigation case ID: {}", investigationCaseId);

        InvestigationCase investigationCase = investigationCaseRepository.findById(investigationCaseId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationCaseId));

        return evidenceRepository.findByInvestigationCaseOrderByUploadedAtDesc(investigationCase);
    }

    /**
     * Get evidence by ID.
     */
    public Evidence getEvidenceById(Long evidenceId) {
        log.info("Fetching evidence ID: {}", evidenceId);

        return evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new RuntimeException("Evidence not found with ID: " + evidenceId));
    }

    /**
     * Delete evidence file.
     */
    public void deleteEvidence(Long evidenceId, String deletedBy) {
        log.info("Deleting evidence ID: {} by user: {}", evidenceId, deletedBy);

        Evidence evidence = getEvidenceById(evidenceId);

        try {
            // Delete physical file
            Path filePath = Paths.get(evidence.getFilePath());
            Files.deleteIfExists(filePath);

            // Create activity record before deletion
            InvestigationActivity activity = new InvestigationActivity(
                    evidence.getInvestigationCase(),
                    InvestigationActivity.ActivityType.CUSTOM,
                    "Evidence file '" + evidence.getOriginalFileName() + "' deleted",
                    deletedBy,
                    true
            );
            investigationActivityRepository.save(activity);

            // Delete from database
            evidenceRepository.delete(evidence);

            log.info("Successfully deleted evidence ID: {}", evidenceId);

        } catch (IOException e) {
            log.error("Error deleting evidence file: ", e);
            throw new RuntimeException("Failed to delete evidence file", e);
        }
    }

    /**
     * Update evidence metadata.
     */
    public Evidence updateEvidenceMetadata(Long evidenceId, String description, Set<String> tags,
                                         Boolean isCritical, String updatedBy) {
        log.info("Updating evidence metadata for ID: {}", evidenceId);

        Evidence evidence = getEvidenceById(evidenceId);

        if (description != null) {
            evidence.setDescription(description);
        }

        if (tags != null) {
            evidence.setTags(tags);
        }

        if (isCritical != null) {
            evidence.setIsCritical(isCritical);
        }

        Evidence updated = evidenceRepository.save(evidence);

        // Create activity record
        InvestigationActivity activity = new InvestigationActivity(
                evidence.getInvestigationCase(),
                InvestigationActivity.ActivityType.CUSTOM,
                "Evidence metadata updated for '" + evidence.getOriginalFileName() + "'",
                updatedBy,
                true
        );
        activity.setRelatedEvidenceId(evidenceId);
        investigationActivityRepository.save(activity);

        log.info("Successfully updated evidence metadata for ID: {}", evidenceId);
        return updated;
    }

    /**
     * Search evidence by various criteria.
     */
    public List<Evidence> searchEvidence(Long investigationCaseId, Evidence.EvidenceType fileType,
                                       String searchTerm, Boolean isCritical, String uploadedBy) {
        log.info("Searching evidence for investigation case ID: {} with criteria", investigationCaseId);

        InvestigationCase investigationCase = investigationCaseRepository.findById(investigationCaseId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationCaseId));

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            return evidenceRepository.findByInvestigationCaseAndFileNameContaining(investigationCase, searchTerm.trim());
        }

        return evidenceRepository.findByCriteria(investigationCase, fileType, isCritical, uploadedBy);
    }

    /**
     * Get evidence statistics for a case.
     */
    public EvidenceStatistics getEvidenceStatistics(Long investigationCaseId) {
        log.info("Fetching evidence statistics for investigation case ID: {}", investigationCaseId);

        InvestigationCase investigationCase = investigationCaseRepository.findById(investigationCaseId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationCaseId));

        long totalFiles = evidenceRepository.countByInvestigationCase(investigationCase);
        long totalSize = evidenceRepository.getTotalFileSizeByCase(investigationCase);

        // Count by type
        long photoCount = evidenceRepository.countByInvestigationCaseAndFileType(investigationCase, Evidence.EvidenceType.PHOTO);
        long documentCount = evidenceRepository.countByInvestigationCaseAndFileType(investigationCase, Evidence.EvidenceType.DOCUMENT);
        long reportCount = evidenceRepository.countByInvestigationCaseAndFileType(investigationCase, Evidence.EvidenceType.REPORT);
        long communicationCount = evidenceRepository.countByInvestigationCaseAndFileType(investigationCase, Evidence.EvidenceType.COMMUNICATION);

        return new EvidenceStatistics(totalFiles, totalSize, photoCount, documentCount, reportCount, communicationCount);
    }

    // Helper methods
    private void validateFileUpload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        String contentType = file.getContentType();
        if (!isAllowedFileType(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }
    }

    private boolean isAllowedFileType(String contentType) {
        return contentType != null && (
                ALLOWED_IMAGE_TYPES.contains(contentType) ||
                ALLOWED_DOCUMENT_TYPES.contains(contentType) ||
                ALLOWED_VIDEO_TYPES.contains(contentType) ||
                ALLOWED_AUDIO_TYPES.contains(contentType)
        );
    }

    private Evidence.EvidenceType determineEvidenceType(String mimeType) {
        if (mimeType == null) return Evidence.EvidenceType.OTHER;

        if (ALLOWED_IMAGE_TYPES.contains(mimeType)) {
            return Evidence.EvidenceType.PHOTO;
        } else if (ALLOWED_DOCUMENT_TYPES.contains(mimeType)) {
            return Evidence.EvidenceType.DOCUMENT;
        } else if (ALLOWED_VIDEO_TYPES.contains(mimeType)) {
            return Evidence.EvidenceType.VIDEO;
        } else if (ALLOWED_AUDIO_TYPES.contains(mimeType)) {
            return Evidence.EvidenceType.AUDIO;
        }

        return Evidence.EvidenceType.OTHER;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private Path createUploadPath(Long investigationCaseId) throws IOException {
        Path uploadPath = Paths.get(evidenceUploadDir, "case_" + investigationCaseId);
        Files.createDirectories(uploadPath);
        return uploadPath;
    }

    /**
     * Inner class for evidence statistics.
     */
    public static class EvidenceStatistics {
        private final long totalFiles;
        private final long totalSizeBytes;
        private final long photoCount;
        private final long documentCount;
        private final long reportCount;
        private final long communicationCount;

        public EvidenceStatistics(long totalFiles, long totalSizeBytes, long photoCount,
                                long documentCount, long reportCount, long communicationCount) {
            this.totalFiles = totalFiles;
            this.totalSizeBytes = totalSizeBytes;
            this.photoCount = photoCount;
            this.documentCount = documentCount;
            this.reportCount = reportCount;
            this.communicationCount = communicationCount;
        }

        // Getters
        public long getTotalFiles() { return totalFiles; }
        public long getTotalSizeBytes() { return totalSizeBytes; }
        public long getPhotoCount() { return photoCount; }
        public long getDocumentCount() { return documentCount; }
        public long getReportCount() { return reportCount; }
        public long getCommunicationCount() { return communicationCount; }

        public String getFormattedTotalSize() {
            double bytes = totalSizeBytes;
            String[] units = {"B", "KB", "MB", "GB"};
            int unitIndex = 0;

            while (bytes >= 1024 && unitIndex < units.length - 1) {
                bytes /= 1024;
                unitIndex++;
            }

            return String.format("%.1f %s", bytes, units[unitIndex]);
        }
    }
}