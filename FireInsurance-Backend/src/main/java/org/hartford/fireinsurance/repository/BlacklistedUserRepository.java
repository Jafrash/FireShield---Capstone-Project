package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.BlacklistedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlacklistedUserRepository extends JpaRepository<BlacklistedUser, Long> {
    // Check if any identifier is blacklisted (active only)
    boolean existsByUsernameAndActiveTrue(String username);
    boolean existsByEmailAndActiveTrue(String email);
    boolean existsByPhoneAndActiveTrue(String phone);

    // OR-based lookup for all three
    boolean existsByUsernameOrEmailOrPhoneAndActiveTrue(String username, String email, String phone);

    // Find by username
    Optional<BlacklistedUser> findByUsernameAndActiveTrue(String username);

    // Find by email
    Optional<BlacklistedUser> findByEmailAndActiveTrue(String email);

    // Find by phone
    Optional<BlacklistedUser> findByPhoneAndActiveTrue(String phone);

    // Check if user is blacklisted by any identifier
        @Query("SELECT b FROM BlacklistedUser b WHERE b.active = true AND " +
            "(b.username = :identifier OR b.email = :identifier OR b.phone = :identifier)")
        List<BlacklistedUser> findByAnyIdentifier(@Param("identifier") String identifier);

    // Get all active blacklisted users
    List<BlacklistedUser> findByActiveTrue();

    // Find by created by admin
    List<BlacklistedUser> findByCreatedByAndActiveTrue(String createdBy);
}