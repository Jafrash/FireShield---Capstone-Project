package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.BlacklistRequest;
import org.hartford.fireinsurance.model.BlacklistedUser;
import org.hartford.fireinsurance.repository.BlacklistedUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BlacklistService {
        /**
         * Check if a user is blacklisted by username, email, or phone (OR logic, null-safe)
         */
        public boolean isBlacklisted(String username, String email, String phone) {
            if ((username == null || username.isBlank()) && (email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
                return false;
            }
            return blacklistedUserRepository.existsByUsernameOrEmailOrPhoneAndActiveTrue(
                username != null ? username : "",
                email != null ? email : "",
                phone != null ? phone : ""
            );
        }
    private static final Logger log = LoggerFactory.getLogger(BlacklistService.class);

    private final BlacklistedUserRepository blacklistedUserRepository;

    public BlacklistService(BlacklistedUserRepository blacklistedUserRepository) {
        this.blacklistedUserRepository = blacklistedUserRepository;
    }

    /**
     * Add a user to the blacklist
     */
    public BlacklistedUser addToBlacklist(BlacklistRequest request, String adminUsername) {
        if (blacklistedUserRepository.findByUsernameAndActiveTrue(request.getUsername()).isPresent()) {
            throw new RuntimeException("User with username '" + request.getUsername() + "' is already blacklisted");
        }
        if (blacklistedUserRepository.findByEmailAndActiveTrue(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with email '" + request.getEmail() + "' is already blacklisted");
        }
        // Create new blacklist entry
        BlacklistedUser blacklistedUser = new BlacklistedUser();
        blacklistedUser.setUsername(request.getUsername());
        blacklistedUser.setEmail(request.getEmail());
        blacklistedUser.setPhone(request.getPhoneNumber());
        blacklistedUser.setReason(request.getReason());
        blacklistedUser.setCreatedBy(adminUsername);
        blacklistedUser.setActive(true);
        blacklistedUser.setCreatedAt(LocalDateTime.now());
        BlacklistedUser saved = blacklistedUserRepository.save(blacklistedUser);
        log.info("Successfully added user to blacklist: ID={}", saved.getBlacklistId());
        return saved;
    }

    /**
     * Get all active blacklisted users
     */
    public List<BlacklistedUser> getAllBlacklistedUsers() {
        log.debug("Fetching all active blacklisted users");
        return blacklistedUserRepository.findByActiveTrue();
    }

    /**
     * Get blacklist entry by ID
     */
    public BlacklistedUser getBlacklistedUserById(Long id) {
        return blacklistedUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blacklisted user not found with ID: " + id));
    }

    /**
     * Remove user from blacklist (soft delete)
     */
    public void removeFromBlacklist(Long id, String adminUsername) {
        log.info("Removing user from blacklist: ID={}, admin={}", id, adminUsername);

        BlacklistedUser blacklistedUser = getBlacklistedUserById(id);
        blacklistedUser.setActive(false);
        blacklistedUserRepository.save(blacklistedUser);

        log.info("Successfully removed user from blacklist: ID={}", id);
    }

    /**
     * Check if a user is blacklisted by any identifier (username, email, or phone)
     */
    public boolean isUserBlacklisted(String identifier) {
        List<BlacklistedUser> matches = blacklistedUserRepository.findByAnyIdentifier(identifier);
        boolean isBlacklisted = !matches.isEmpty();

        if (isBlacklisted) {
            log.warn("User is blacklisted: identifier={}, matches={}", identifier, matches.size());
        }

        return isBlacklisted;
    }

    /**
     * Get blacklisted users created by specific admin
     */
    public List<BlacklistedUser> getBlacklistedUsersByAdmin(String adminUsername) {
        return blacklistedUserRepository.findByCreatedByAndActiveTrue(adminUsername);
    }

    /**
     * Update blacklist entry reason
     */
    public BlacklistedUser updateBlacklistReason(Long id, String newReason, String adminUsername) {
        log.info("Updating blacklist reason: ID={}, admin={}", id, adminUsername);

        BlacklistedUser blacklistedUser = getBlacklistedUserById(id);
        blacklistedUser.setReason(newReason);

        BlacklistedUser updated = blacklistedUserRepository.save(blacklistedUser);
        log.info("Successfully updated blacklist reason: ID={}", id);

        return updated;
    }
}
