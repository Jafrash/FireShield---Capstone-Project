package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.SiuInvestigator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SIU Investigator entities
 */
@Repository
public interface SiuInvestigatorRepository extends JpaRepository<SiuInvestigator, Long> {

    /**
     * Find all active SIU investigators
     */
    List<SiuInvestigator> findByActiveTrue();

    /**
     * Find SIU investigator by user ID
     */
    Optional<SiuInvestigator> findByUser_Id(Long userId);

    /**
     * Find SIU investigator by badge number
     */
    Optional<SiuInvestigator> findByBadgeNumber(String badgeNumber);

    /**
     * Find all active investigators ordered by username
     */
    @Query("SELECT s FROM SiuInvestigator s WHERE s.active = true ORDER BY s.user.username")
    List<SiuInvestigator> findActiveInvestigatorsOrderByName();

    /**
     * Check if badge number exists
     */
    boolean existsByBadgeNumber(String badgeNumber);

    /**
     * Count active investigators
     */
    long countByActiveTrue();
}