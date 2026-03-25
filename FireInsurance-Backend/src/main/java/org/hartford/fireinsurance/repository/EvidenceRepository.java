package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Evidence;
import org.hartford.fireinsurance.model.InvestigationCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for managing Evidence entities.
 * Provides query methods for evidence management in SIU investigations.
 */
@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    // Find evidence by investigation case
    List<Evidence> findByInvestigationCase(InvestigationCase investigationCase);

    // Find evidence by investigation case ordered by upload date
    List<Evidence> findByInvestigationCaseOrderByUploadedAtDesc(InvestigationCase investigationCase);

    // Find evidence by file type
    List<Evidence> findByFileType(Evidence.EvidenceType fileType);

    // Find evidence by investigation case and file type
    List<Evidence> findByInvestigationCaseAndFileType(InvestigationCase investigationCase,
                                                     Evidence.EvidenceType fileType);

    // Find critical evidence
    @Query("SELECT e FROM Evidence e WHERE e.investigationCase = :investigationCase AND e.isCritical = true")
    List<Evidence> findCriticalEvidenceByCase(@Param("investigationCase") InvestigationCase investigationCase);

    // Find evidence uploaded by specific user
    List<Evidence> findByUploadedBy(String uploadedBy);

    // Find evidence uploaded within date range
    @Query("SELECT e FROM Evidence e WHERE e.investigationCase = :investigationCase " +
           "AND e.uploadedAt >= :startDate AND e.uploadedAt <= :endDate")
    List<Evidence> findByInvestigationCaseAndUploadedAtBetween(
            @Param("investigationCase") InvestigationCase investigationCase,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Search evidence by tags
    @Query("SELECT DISTINCT e FROM Evidence e JOIN e.tags t WHERE e.investigationCase = :investigationCase " +
           "AND LOWER(t) LIKE LOWER(CONCAT('%', :tag, '%'))")
    List<Evidence> findByInvestigationCaseAndTagsContaining(
            @Param("investigationCase") InvestigationCase investigationCase,
            @Param("tag") String tag);

    // Find evidence by file name pattern
    @Query("SELECT e FROM Evidence e WHERE e.investigationCase = :investigationCase " +
           "AND (LOWER(e.fileName) LIKE LOWER(CONCAT('%', :fileName, '%')) " +
           "OR LOWER(e.originalFileName) LIKE LOWER(CONCAT('%', :fileName, '%')))")
    List<Evidence> findByInvestigationCaseAndFileNameContaining(
            @Param("investigationCase") InvestigationCase investigationCase,
            @Param("fileName") String fileName);

    // Count evidence by investigation case
    long countByInvestigationCase(InvestigationCase investigationCase);

    // Count evidence by investigation case and file type
    long countByInvestigationCaseAndFileType(InvestigationCase investigationCase,
                                           Evidence.EvidenceType fileType);

    // Get total file size for investigation case
    @Query("SELECT COALESCE(SUM(e.fileSize), 0) FROM Evidence e WHERE e.investigationCase = :investigationCase")
    Long getTotalFileSizeByCase(@Param("investigationCase") InvestigationCase investigationCase);

    // Find recent evidence (last 7 days)
    @Query("SELECT e FROM Evidence e WHERE e.investigationCase = :investigationCase " +
           "AND e.uploadedAt >= :sinceDate ORDER BY e.uploadedAt DESC")
    List<Evidence> findRecentEvidenceByCase(
            @Param("investigationCase") InvestigationCase investigationCase,
            @Param("sinceDate") LocalDateTime sinceDate);

    // Find evidence by multiple criteria
    @Query("SELECT e FROM Evidence e WHERE e.investigationCase = :investigationCase " +
           "AND (:fileType IS NULL OR e.fileType = :fileType) " +
           "AND (:isCritical IS NULL OR e.isCritical = :isCritical) " +
           "AND (:uploadedBy IS NULL OR e.uploadedBy = :uploadedBy) " +
           "ORDER BY e.uploadedAt DESC")
    List<Evidence> findByCriteria(
            @Param("investigationCase") InvestigationCase investigationCase,
            @Param("fileType") Evidence.EvidenceType fileType,
            @Param("isCritical") Boolean isCritical,
            @Param("uploadedBy") String uploadedBy);
}