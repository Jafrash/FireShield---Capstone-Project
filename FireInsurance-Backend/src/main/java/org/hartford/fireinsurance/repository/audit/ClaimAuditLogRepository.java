package org.hartford.fireinsurance.repository.audit;

import org.hartford.fireinsurance.model.ClaimAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ClaimAuditLog entities.
 * Provides querying capabilities for audit trail analysis and compliance reporting.
 */
@Repository
public interface ClaimAuditLogRepository extends JpaRepository<ClaimAuditLog, Long> {

    /**
     * Find all audit logs for a specific claim.
     */
    List<ClaimAuditLog> findByClaimIdOrderByPerformedAtDesc(Long claimId);

    /**
     * Find audit logs for a claim within a date range.
     */
    @Query("SELECT a FROM ClaimAuditLog a WHERE a.claimId = :claimId " +
           "AND a.performedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.performedAt DESC")
    List<ClaimAuditLog> findByClaimIdAndDateRange(
        @Param("claimId") Long claimId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * Find audit logs by event type.
     */
    Page<ClaimAuditLog> findByEventType(
        ClaimAuditLog.AuditEventType eventType, Pageable pageable);

    /**
     * Find audit logs by performer (user).
     */
    Page<ClaimAuditLog> findByPerformedByContainingIgnoreCase(
        String performedBy, Pageable pageable);

    /**
     * Find high-risk audit events.
     */
    @Query("SELECT a FROM ClaimAuditLog a WHERE a.riskLevel IN ('HIGH', 'CRITICAL') " +
           "ORDER BY a.performedAt DESC")
    List<ClaimAuditLog> findHighRiskEvents(Pageable pageable);

    /**
     * Find security events for compliance reporting.
     */
    List<ClaimAuditLog> findByEventTypeAndPerformedAtAfter(
        ClaimAuditLog.AuditEventType eventType, LocalDateTime since);

    /**
     * Count audit events by type for metrics.
     */
    @Query("SELECT a.eventType, COUNT(a) FROM ClaimAuditLog a " +
           "WHERE a.performedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY a.eventType")
    List<Object[]> countEventsByType(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * Find claims with suspicious audit patterns (multiple high-risk events).
     */
    @Query("SELECT a.claimId, COUNT(a) FROM ClaimAuditLog a " +
           "WHERE a.riskLevel IN ('HIGH', 'CRITICAL') " +
           "AND a.performedAt > :since " +
           "GROUP BY a.claimId " +
           "HAVING COUNT(a) >= :threshold " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> findSuspiciousAuditPatterns(
        @Param("since") LocalDateTime since,
        @Param("threshold") Long threshold);
}