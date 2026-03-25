package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Blacklist;
import org.hartford.fireinsurance.model.Blacklist.BlacklistType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Blacklist entities for fraud detection.
 */
public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {

    /**
     * Find all active blacklist entries.
     */
    List<Blacklist> findByIsActiveTrue();

    /**
     * Find all blacklist entries by type.
     */
    List<Blacklist> findByType(BlacklistType type);

    /**
     * Find active blacklist entries by type.
     */
    List<Blacklist> findByTypeAndIsActiveTrue(BlacklistType type);

    /**
     * Find a blacklist entry by type and value.
     */
    Optional<Blacklist> findByTypeAndValue(BlacklistType type, String value);

    /**
     * Find active blacklist entry by type and value (case-insensitive).
     */
    @Query("SELECT b FROM Blacklist b WHERE b.type = :type AND LOWER(b.value) = LOWER(:value) AND b.isActive = true")
    Optional<Blacklist> findActiveByTypeAndValue(@Param("type") BlacklistType type, @Param("value") String value);

    /**
     * Check if an active blacklist entry exists by type and value.
     */
    boolean existsByTypeAndValueAndIsActiveTrue(BlacklistType type, String value);

    /**
     * Check if an active blacklist entry exists by type and value (case-insensitive).
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Blacklist b " +
           "WHERE b.type = :type AND LOWER(b.value) = LOWER(:value) AND b.isActive = true")
    boolean existsActiveByTypeAndValueIgnoreCase(@Param("type") BlacklistType type, @Param("value") String value);
}
