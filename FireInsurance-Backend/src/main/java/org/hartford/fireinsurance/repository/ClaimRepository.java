package org.hartford.fireinsurance.repository;


import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Underwriter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim,Long> {

    List<Claim> findBySubscription(PolicySubscription subscription);

    // Find all claims for a specific customer
    @Query("SELECT c FROM Claim c WHERE c.subscription.customer = :customer")
    List<Claim> findByCustomer(@Param("customer") Customer customer);

    // Optimistic locking method for concurrent updates
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Claim c WHERE c.claimId = :claimId")
    Optional<Claim> findByIdWithLock(@Param("claimId") Long claimId);

    List<Claim> findByUnderwriter(Underwriter underwriter);
    List<Claim> findByUnderwriterAndStatus(Underwriter underwriter, Claim.ClaimStatus status);

    // Fraud Detection Queries
    @Query("SELECT COUNT(c) FROM Claim c WHERE c.subscription.customer.customerId = :customerId AND c.createdAt >= :sinceDate")
    long countClaimsByCustomerSince(@Param("customerId") Long customerId, @Param("sinceDate") LocalDateTime sinceDate);

    // Enhanced fraud detection - get claims list for pattern analysis
    @Query("SELECT c FROM Claim c WHERE c.subscription.customer.customerId = :customerId AND c.createdAt >= :sinceDate")
    List<Claim> findClaimsByCustomerSince(@Param("customerId") Long customerId, @Param("sinceDate") LocalDateTime sinceDate);

    // Pattern detection - similar amounts (for fraud rings)
    @Query("SELECT COUNT(c) FROM Claim c WHERE ABS(c.claimAmount - :amount) <= :tolerance AND c.createdAt >= :sinceDate")
    long countClaimsWithSimilarAmount(@Param("amount") Double amount, @Param("tolerance") Double tolerance, @Param("sinceDate") LocalDateTime sinceDate);

    // Pattern detection - coordinated timing
    @Query("SELECT COUNT(c) FROM Claim c WHERE c.createdAt BETWEEN :startTime AND :endTime AND c.createdAt >= :sinceDate")
    long countClaimsNearTime(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("sinceDate") LocalDateTime sinceDate);

    // Geographic anomaly detection - claims in same postal area
    @Query("SELECT COUNT(c) FROM Claim c WHERE c.subscription.property.address LIKE %:postalArea% AND c.createdAt >= :sinceDate")
    long countClaimsInPostalArea(@Param("postalArea") String postalArea, @Param("sinceDate") LocalDateTime sinceDate);

    List<Claim> findByFraudStatus(Claim.FraudStatus fraudStatus);

    List<Claim> findByRiskLevel(Claim.RiskLevel riskLevel);

    @Query("SELECT c FROM Claim c WHERE c.siuInvestigator.investigatorId = :investigatorId")
    List<Claim> findBySiuInvestigatorId(@Param("investigatorId") Long investigatorId);

    // Enhanced query for assigned investigator (used by RefactoredClaimService)
    @Query("SELECT c FROM Claim c WHERE c.siuInvestigator.investigatorId = :investigatorId")
    List<Claim> findByAssignedInvestigator(@Param("investigatorId") Long investigatorId);

    // SIU Investigation Queries
    @Query("SELECT c FROM Claim c WHERE c.fraudStatus = 'SIU_INVESTIGATION'")
    List<Claim> findSiuCases();
}
