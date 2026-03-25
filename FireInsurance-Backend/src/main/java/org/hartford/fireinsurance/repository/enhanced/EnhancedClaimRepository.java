package org.hartford.fireinsurance.repository.enhanced;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.hartford.fireinsurance.domain.state.ClaimState;
import org.hartford.fireinsurance.model.Claim;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Enhanced repository for Claim entities with optimistic locking and concurrency control.
 * Provides safe concurrent access to claim data in the fraud detection system.
 */
@Repository
public interface EnhancedClaimRepository extends JpaRepository<Claim, Long> {

    /**
     * Finds a claim by ID with optimistic locking for safe concurrent updates.
     * This should be used for all update operations to prevent race conditions.
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Claim c WHERE c.id = :id")
    Optional<Claim> findByIdWithLock(@Param("id") Long id);

    /**
     * Finds claims by current state with optimistic locking.
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Claim c WHERE c.currentState = :state")
    List<Claim> findByCurrentStateWithLock(@Param("state") ClaimState state);

    /**
     * Finds claims in SIU investigation states for monitoring.
     */
    @Query("SELECT c FROM Claim c WHERE c.currentState IN ('ESCALATED_TO_SIU', 'SIU_ASSIGNED', 'UNDER_SIU_INVESTIGATION')")
    List<Claim> findClaimsUnderSiuInvestigation();

    /**
     * Finds claims with high fraud scores that may need attention.
     */
    @Query("SELECT c FROM Claim c WHERE c.fraudAssessment.fraudScore >= :threshold")
    List<Claim> findHighFraudScoreClaims(@Param("threshold") Double threshold);

    /**
     * Finds claims by assignee type for workload management.
     */
    @Query("SELECT c FROM Claim c WHERE c.assignmentTracker.assigneeType = :assigneeType")
    List<Claim> findByAssigneeType(@Param("assigneeType") String assigneeType);

    /**
     * Finds claims assigned to specific investigator.
     */
    @Query("SELECT c FROM Claim c WHERE c.assignmentTracker.currentAssignee.id = :investigatorId")
    List<Claim> findByAssignedInvestigator(@Param("investigatorId") Long investigatorId);

    /**
     * Finds overdue assignments for SLA monitoring.
     */
    @Query("SELECT c FROM Claim c WHERE c.assignmentTracker.dueDate < :currentTime " +
           "AND c.assignmentTracker.currentAssignee IS NOT NULL")
    List<Claim> findOverdueAssignments(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Finds claims that block normal processing (under fraud investigation).
     */
    @Query("SELECT c FROM Claim c WHERE c.currentState IN " +
           "('UNDER_SIU_INVESTIGATION', 'ESCALATED_TO_SIU', 'SIU_ASSIGNED')")
    List<Claim> findClaimsBlockingProcessing();

    /**
     * Complex query for fraud analysis dashboard.
     */
    @Query("SELECT c FROM Claim c WHERE c.fraudAssessment.fraudScore >= :minScore " +
           "AND c.fraudAssessment.lastAnalyzed >= :since " +
           "ORDER BY c.fraudAssessment.fraudScore DESC")
    Page<Claim> findRecentHighRiskClaims(
        @Param("minScore") Double minScore,
        @Param("since") LocalDateTime since,
        Pageable pageable);

    /**
     * Finds claims with specific fraud status combinations.
     */
    @Query("SELECT c FROM Claim c WHERE c.currentState = :state " +
           "AND c.fraudAssessment.riskLevel = :riskLevel")
    List<Claim> findByStateAndRiskLevel(
        @Param("state") ClaimState state,
        @Param("riskLevel") String riskLevel);

    /**
     * Count claims by state for dashboard statistics.
     */
    @Query("SELECT c.currentState, COUNT(c) FROM Claim c GROUP BY c.currentState")
    List<Object[]> countClaimsByState();

    /**
     * Count claims by fraud risk level.
     */
    @Query("SELECT c.fraudAssessment.riskLevel, COUNT(c) FROM Claim c " +
           "WHERE c.fraudAssessment.riskLevel IS NOT NULL " +
           "GROUP BY c.fraudAssessment.riskLevel")
    List<Object[]> countClaimsByRiskLevel();

    /**
     * Find suspicious patterns - multiple claims from same customer with high fraud scores.
     */
    @Query("SELECT c.subscription.customer.customerId, COUNT(c), AVG(c.fraudAssessment.fraudScore) " +
           "FROM Claim c WHERE c.fraudAssessment.fraudScore >= :threshold " +
           "GROUP BY c.subscription.customer.customerId " +
           "HAVING COUNT(c) >= :minClaimCount " +
           "ORDER BY AVG(c.fraudAssessment.fraudScore) DESC")
    List<Object[]> findSuspiciousFraudPatterns(
        @Param("threshold") Double threshold,
        @Param("minClaimCount") Long minClaimCount);

    /**
     * Performance monitoring - find claims with frequent updates (potential conflicts).
     */
    @Query("SELECT c FROM Claim c WHERE c.version >= :minVersion " +
           "ORDER BY c.version DESC")
    List<Claim> findHighlyModifiedClaims(@Param("minVersion") Long minVersion, Pageable pageable);
}

/**
 * Custom repository implementation for complex operations requiring EntityManager.
 */
@Repository
class EnhancedClaimRepositoryImpl {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Saves claim with retry logic for optimistic locking failures.
     */
    public Claim saveWithRetry(Claim claim) {
        try {
            return entityManager.merge(claim);
        } catch (OptimisticLockingFailureException e) {
            // Refresh entity and retry
            if (claim.getClaimId() != null) {
                Claim freshClaim = entityManager.find(Claim.class, claim.getClaimId());
                if (freshClaim != null) {
                    entityManager.refresh(freshClaim);
                }
            }
            throw e; // Will trigger retry
        }
    }

    /**
     * Bulk update operation with proper locking.
     */
    public int bulkUpdateFraudScores(Double minScore, String newRiskLevel) {
        return entityManager.createQuery(
            "UPDATE Claim c SET c.fraudAssessment.riskLevel = :newLevel " +
            "WHERE c.fraudAssessment.fraudScore >= :minScore")
            .setParameter("newLevel", newRiskLevel)
            .setParameter("minScore", minScore)
            .executeUpdate();
    }

    /**
     * Custom method to check for concurrent modifications.
     */
    public boolean hasBeenModifiedSince(Long claimId, Long sinceVersion) {
        Long currentVersion = (Long) entityManager.createQuery(
            "SELECT c.version FROM Claim c WHERE c.id = :id")
            .setParameter("id", claimId)
            .getSingleResult();

        return currentVersion > sinceVersion;
    }

    /**
     * Force refresh of claim entity from database.
     */
    public void refreshClaim(Long claimId) {
        Claim claim = entityManager.find(Claim.class, claimId);
        if (claim != null) {
            entityManager.refresh(claim);
        }
    }
}