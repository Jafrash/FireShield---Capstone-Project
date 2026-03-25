package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.InvestigationCase;
import org.hartford.fireinsurance.model.InvestigationStatus;
import org.hartford.fireinsurance.model.SiuInvestigator;
import org.hartford.fireinsurance.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing InvestigationCase entities.
 * Provides query methods for SIU investigation case management.
 */
@Repository
public interface InvestigationCaseRepository extends JpaRepository<InvestigationCase, Long> {

    // Find cases by assigned investigator
    List<InvestigationCase> findByAssignedInvestigator(SiuInvestigator investigator);

    // Find cases by assigned investigator and status
    List<InvestigationCase> findByAssignedInvestigatorAndStatus(SiuInvestigator investigator, InvestigationStatus status);

    // Find cases by claim
    Optional<InvestigationCase> findByClaim(Claim claim);

    // Find cases by status
    List<InvestigationCase> findByStatus(InvestigationStatus status);

    // Find cases by status ordered by priority and creation date
    List<InvestigationCase> findByStatusOrderByPriorityLevelAscCreatedAtDesc(InvestigationStatus status);

    // Find all cases for a specific investigator ordered by priority and creation date
    @Query("SELECT ic FROM InvestigationCase ic WHERE ic.assignedInvestigator = :investigator " +
           "ORDER BY ic.priorityLevel ASC, ic.createdAt DESC")
    List<InvestigationCase> findByAssignedInvestigatorOrderByPriorityAndDate(@Param("investigator") SiuInvestigator investigator);

    // Find cases assigned to investigator within date range
    @Query("SELECT ic FROM InvestigationCase ic WHERE ic.assignedInvestigator = :investigator " +
           "AND ic.assignedDate >= :startDate AND ic.assignedDate <= :endDate")
    List<InvestigationCase> findByAssignedInvestigatorAndAssignedDateBetween(
            @Param("investigator") SiuInvestigator investigator,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Count active cases by investigator (ASSIGNED or INVESTIGATING)
    @Query("SELECT COUNT(ic) FROM InvestigationCase ic WHERE ic.assignedInvestigator = :investigator " +
           "AND ic.status IN ('ASSIGNED', 'INVESTIGATING')")
    long countActiveCasesByInvestigator(@Param("investigator") SiuInvestigator investigator);

    // Find high priority cases (priority level 1-2) that are not completed
    @Query("SELECT ic FROM InvestigationCase ic WHERE ic.priorityLevel <= 2 " +
           "AND ic.status != 'COMPLETED' ORDER BY ic.priorityLevel ASC, ic.createdAt ASC")
    List<InvestigationCase> findHighPriorityCases();

    // Find cases that have been assigned but not started within specified days
    @Query("SELECT ic FROM InvestigationCase ic WHERE ic.status = 'ASSIGNED' " +
           "AND ic.assignedDate <= :cutoffDate")
    List<InvestigationCase> findOverdueAssignedCases(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Find cases by fraud score range from associated claim
    @Query("SELECT ic FROM InvestigationCase ic WHERE ic.claim.fraudAssessment.fraudScore >= :minScore " +
           "AND ic.claim.fraudAssessment.fraudScore <= :maxScore")
    List<InvestigationCase> findByFraudScoreRange(
            @Param("minScore") Double minScore,
            @Param("maxScore") Double maxScore);

    // Get case statistics by investigator
    @Query("SELECT ic.status, COUNT(ic) FROM InvestigationCase ic WHERE ic.assignedInvestigator = :investigator " +
           "GROUP BY ic.status")
    List<Object[]> getCaseStatisticsByInvestigator(@Param("investigator") SiuInvestigator investigator);

    // Find recent cases (last 30 days) by investigator
    @Query("SELECT ic FROM InvestigationCase ic WHERE ic.assignedInvestigator = :investigator " +
           "AND ic.createdAt >= :sinceDate ORDER BY ic.createdAt DESC")
    List<InvestigationCase> findRecentCasesByInvestigator(
            @Param("investigator") SiuInvestigator investigator,
            @Param("sinceDate") LocalDateTime sinceDate);
}