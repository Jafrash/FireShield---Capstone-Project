package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.BlacklistedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlacklistedUserRepository extends JpaRepository<BlacklistedUser, Long> {

    // Find by username
    Optional<BlacklistedUser> findByUsernameAndActiveTrue(String username);

    // Find by email
    Optional<BlacklistedUser> findByEmailAndActiveTrue(String email);

    // Find by phone number
    Optional<BlacklistedUser> findByPhoneNumberAndActiveTrue(String phoneNumber);

    // Check if user is blacklisted by any identifier
    @Query("SELECT b FROM BlacklistedUser b WHERE b.active = true AND " +
           "(b.username = :identifier OR b.email = :identifier OR b.phoneNumber = :identifier)")
    List<BlacklistedUser> findByAnyIdentifier(@Param("identifier") String identifier);

    // Get all active blacklisted users
    List<BlacklistedUser> findByActiveTrue();

    // Find by created by admin
    List<BlacklistedUser> findByCreatedByAndActiveTrue(String createdBy);
}