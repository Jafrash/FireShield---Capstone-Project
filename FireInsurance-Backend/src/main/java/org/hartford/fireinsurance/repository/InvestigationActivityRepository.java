package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.InvestigationActivity;
import org.hartford.fireinsurance.model.InvestigationCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for managing InvestigationActivity entities.
 */
@Repository
public interface InvestigationActivityRepository extends JpaRepository<InvestigationActivity, Long> {

    // Find activities by investigation case
    List<InvestigationActivity> findByInvestigationCase(InvestigationCase investigationCase);

    // Find activities by investigation case ordered by performed date (chronological timeline)
    List<InvestigationActivity> findByInvestigationCaseOrderByPerformedAtAsc(InvestigationCase investigationCase);

    // Find activities by investigation case ordered by performed date (reverse chronological)
    List<InvestigationActivity> findByInvestigationCaseOrderByPerformedAtDesc(InvestigationCase investigationCase);

    // Find activities by type
    List<InvestigationActivity> findByActivityType(InvestigationActivity.ActivityType activityType);

    // Find activities by investigation case and type
    List<InvestigationActivity> findByInvestigationCaseAndActivityType(InvestigationCase investigationCase,
                                                                      InvestigationActivity.ActivityType activityType);

    // Find activities performed by specific user
    List<InvestigationActivity> findByPerformedBy(String performedBy);

    // Find system-generated activities
    List<InvestigationActivity> findBySystemGeneratedTrue();

    // Find user-generated activities
    List<InvestigationActivity> findBySystemGeneratedFalse();

    // Find activities within date range
    @Query("SELECT a FROM InvestigationActivity a WHERE a.investigationCase = :investigationCase " +
           "AND a.performedAt >= :startDate AND a.performedAt <= :endDate " +
           "ORDER BY a.performedAt ASC")
    List<InvestigationActivity> findByInvestigationCaseAndPerformedAtBetween(
            @Param("investigationCase") InvestigationCase investigationCase,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Find recent activities
    @Query("SELECT a FROM InvestigationActivity a WHERE a.investigationCase = :investigationCase " +
           "AND a.performedAt >= :sinceDate ORDER BY a.performedAt DESC")
    List<InvestigationActivity> findRecentActivitiesByCase(
            @Param("investigationCase") InvestigationCase investigationCase,
            @Param("sinceDate") LocalDateTime sinceDate);

    // Get activity statistics by case
    @Query("SELECT a.activityType, COUNT(a) FROM InvestigationActivity a WHERE a.investigationCase = :investigationCase " +
           "GROUP BY a.activityType ORDER BY COUNT(a) DESC")
    List<Object[]> getActivityStatisticsByCase(@Param("investigationCase") InvestigationCase investigationCase);

    // Count activities by investigation case
    long countByInvestigationCase(InvestigationCase investigationCase);

    // Find activities related to specific evidence
    List<InvestigationActivity> findByRelatedEvidenceId(Long evidenceId);

    // Find activities related to specific note
    List<InvestigationActivity> findByRelatedNoteId(Long noteId);

    // Search activities by description
    @Query("SELECT a FROM InvestigationActivity a WHERE a.investigationCase = :investigationCase " +
           "AND LOWER(a.activityDescription) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<InvestigationActivity> findByInvestigationCaseAndDescriptionContaining(
            @Param("investigationCase") InvestigationCase investigationCase,
            @Param("searchTerm") String searchTerm);

    // Get timeline of all activities for case
    @Query("SELECT a FROM InvestigationActivity a WHERE a.investigationCase = :investigationCase " +
           "ORDER BY a.performedAt ASC, a.activityId ASC")
    List<InvestigationActivity> getTimelineByCase(@Param("investigationCase") InvestigationCase investigationCase);
}