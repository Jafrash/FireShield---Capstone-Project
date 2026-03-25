package org.hartford.fireinsurance.service.concurrency;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

/**
 * Concurrency control service for safe claim operations.
 * Provides optimistic locking with intelligent retry strategies to prevent data corruption
 * and race conditions in the fraud detection system.
 *
 * KEY FEATURES:
 * - Optimistic locking with version control
 * - Automatic retry logic with exponential backoff
 * - Conflict detection and resolution
 * - Performance monitoring and logging
 */
@Service
public class ConcurrencyControlService {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyControlService.class);

    private final ClaimRepository claimRepository;
    private final EntityManager entityManager;

    @Autowired
    public ConcurrencyControlService(ClaimRepository claimRepository, EntityManager entityManager) {
        this.claimRepository = claimRepository;
        this.entityManager = entityManager;
    }

    /**
     * Updates a claim with automatic retry logic for optimistic locking failures.
     * This is the primary method for safe claim modifications.
     *
     * @param claimId the claim to update
     * @param updateFunction function that performs the update
     * @return updated claim
     * @throws ConcurrencyException if update fails after retries
     */
    @Retryable(
        value = {OptimisticLockingFailureException.class, OptimisticLockException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2.0, random = true)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Claim updateClaimWithRetry(Long claimId, Function<Claim, Claim> updateFunction) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Starting concurrent update for claim: {}", claimId);

            // 1. Load claim with optimistic lock
            Claim claim = claimRepository.findByIdWithLock(claimId)
                .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + claimId));

            Long originalVersion = claim.getVersion();
            log.debug("Loaded claim {} with version: {}", claimId, originalVersion);

            // 2. Apply update function
            Claim updatedClaim = updateFunction.apply(claim);

            // 3. Save with version check
            updatedClaim = claimRepository.save(updatedClaim);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Successfully updated claim {} (version {} -> {}) in {} ms",
                claimId, originalVersion, updatedClaim.getVersion(), duration);

            return updatedClaim;

        } catch (OptimisticLockingFailureException | OptimisticLockException e) {
            log.warn("Optimistic locking failure for claim {}: {}", claimId, e.getMessage());

            // Refresh entity before retry
            refreshClaimEntity(claimId);
            throw e; // Will trigger retry

        } catch (Exception e) {
            log.error("Unexpected error during claim update {}: {}", claimId, e.getMessage(), e);
            throw new ConcurrencyException("Failed to update claim", e);
        }
    }

    /**
     * Updates multiple claims atomically with conflict detection.
     * Use this for operations that need to modify multiple related claims.
     *
     * @param claimIds claims to update
     * @param updateFunction function applied to each claim
     * @return updated claims
     */
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 200, multiplier = 1.5)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateMultipleClaimsWithRetry(Long[] claimIds, Function<Claim, Claim> updateFunction) {
        try {
            log.info("Starting batch update for {} claims", claimIds.length);

            for (Long claimId : claimIds) {
                Claim claim = claimRepository.findByIdWithLock(claimId)
                    .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + claimId));

                Claim updatedClaim = updateFunction.apply(claim);
                claimRepository.save(updatedClaim);
            }

            log.info("Successfully updated {} claims in batch", claimIds.length);

        } catch (OptimisticLockingFailureException e) {
            log.warn("Batch update conflict detected, retrying...");
            throw e; // Will trigger retry
        }
    }

    /**
     * Performs a conditional update only if the claim is in expected state.
     * This provides additional safety for critical operations.
     *
     * @param claimId claim to update
     * @param expectedVersion expected version for optimistic locking
     * @param updateFunction update to apply
     * @return updated claim
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Claim conditionalUpdate(Long claimId, Long expectedVersion, Function<Claim, Claim> updateFunction) {
        Claim claim = claimRepository.findByIdWithLock(claimId)
            .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + claimId));

        if (!claim.getVersion().equals(expectedVersion)) {
            throw new ConcurrencyConflictException(
                String.format("Version mismatch for claim %d: expected %d, found %d",
                    claimId, expectedVersion, claim.getVersion()));
        }

        return claimRepository.save(updateFunction.apply(claim));
    }

    /**
     * Checks if a claim has been modified since a specific version.
     * Useful for detecting conflicts before attempting updates.
     */
    public boolean hasBeenModifiedSince(Long claimId, Long sinceVersion) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + claimId));

        boolean modified = claim.getVersion() > sinceVersion;

        if (modified) {
            log.debug("Claim {} has been modified: version {} > {}", claimId, claim.getVersion(), sinceVersion);
        }

        return modified;
    }

    /**
     * Gets the current version of a claim for optimistic locking.
     */
    public Long getCurrentVersion(Long claimId) {
        return claimRepository.findById(claimId)
            .map(Claim::getVersion)
            .orElseThrow(() -> new ClaimNotFoundException("Claim not found: " + claimId));
    }

    /**
     * Refreshes a claim entity to get the latest version from database.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshClaimEntity(Long claimId) {
        try {
            Claim claim = claimRepository.findById(claimId).orElse(null);
            if (claim != null) {
                entityManager.refresh(claim);
                log.debug("Refreshed claim entity: {} to version: {}", claimId, claim.getVersion());
            }
        } catch (Exception e) {
            log.warn("Could not refresh claim entity {}: {}", claimId, e.getMessage());
        }
    }

    /**
     * Detects potential deadlock conditions by checking claim access patterns.
     * This is a monitoring method to help identify concurrency issues.
     */
    public ConcurrencyHealthReport generateConcurrencyHealthReport() {
        // TODO: Implement based on audit logs and performance metrics
        return new ConcurrencyHealthReport();
    }

    /**
     * Clears entity manager cache to prevent stale data issues.
     * Use sparingly as it impacts performance.
     */
    @Transactional
    public void clearEntityCache() {
        entityManager.clear();
        log.info("Entity manager cache cleared");
    }

    // Exception classes

    public static class ConcurrencyException extends RuntimeException {
        public ConcurrencyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ConcurrencyConflictException extends ConcurrencyException {
        public ConcurrencyConflictException(String message) {
            super(message, null);
        }
    }

    public static class ClaimNotFoundException extends ConcurrencyException {
        public ClaimNotFoundException(String message) {
            super(message, null);
        }
    }

    /**
     * Health report for monitoring concurrency performance.
     */
    public static class ConcurrencyHealthReport {
        private final long timestamp = System.currentTimeMillis();
        private int conflictCount = 0;
        private int retryCount = 0;
        private double averageUpdateTime = 0.0;

        // Getters and analysis methods would be implemented here
        public long getTimestamp() { return timestamp; }
        public int getConflictCount() { return conflictCount; }
        public int getRetryCount() { return retryCount; }
        public double getAverageUpdateTime() { return averageUpdateTime; }
    }
}