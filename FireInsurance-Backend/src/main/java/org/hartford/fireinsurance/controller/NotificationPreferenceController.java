package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.NotificationPreferenceDTO;
import org.hartford.fireinsurance.model.NotificationPreference;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.NotificationPreferenceRepository;
import org.hartford.fireinsurance.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    // Valid event keys for v1
    private static final Set<String> VALID_EVENT_KEYS = new HashSet<>(Arrays.asList(
            "POLICY_SUBMITTED",
            "CLAIM_STATUS_CHANGE",
            "INSPECTION_ASSIGNED",
            "INSPECTION_COMPLETED",
            "POLICY_APPROVAL",
            "POLICY_REJECTION",
            "POLICY_RENEWAL_REMINDER"
    ));

    public NotificationPreferenceController(
            NotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository
    ) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/notification-preferences - Retrieve current user's email notification preferences.
     * Returns user's preferences if authenticated, or default preferences if not.
     */
    @GetMapping("")
    public ResponseEntity<NotificationPreferenceDTO> getPreferences() {
        String username = getAuthenticatedUsername();
        
        // If user is not authenticated, return default preferences
        if (username == null) {
            NotificationPreferenceDTO defaultPrefs = new NotificationPreferenceDTO();
            defaultPrefs.setEmailEnabled(false);
            defaultPrefs.setEnabledEventKeys(new HashSet<>());
            return ResponseEntity.ok(defaultPrefs);
        }

        NotificationPreference preference = preferenceRepository.findByUserUsername(username)
                .orElse(null);

        if (preference == null) {
            // Create default preferences for user if not found
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            preference = new NotificationPreference(user);
            preferenceRepository.save(preference);
        }

        return ResponseEntity.ok(mapToDTO(preference));
    }

    /**
     * PUT /api/notification-preferences - Update current user's email notification preferences.
     * Requires authentication.
     * Validates event keys and enforces user ownership.
     */
    @PutMapping("")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPreferenceDTO> updatePreferences(
            @RequestBody NotificationPreferenceDTO dto
    ) {
        String username = getAuthenticatedUsername();
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Ownership check: ensure user cannot update another user's preferences
        if (dto.getUsername() != null && !dto.getUsername().equals(username)) {
            log.warn("User {} attempted to update preferences for user {}", username, dto.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        NotificationPreference preference = preferenceRepository.findByUser(user)
                .orElse(new NotificationPreference(user));

        // Update emailEnabled flag
        if (dto.getEmailEnabled() != null) {
            preference.setEmailEnabled(dto.getEmailEnabled());
        }

        // Validate and update enabled event keys
        if (dto.getEnabledEventKeys() != null && !dto.getEnabledEventKeys().isEmpty()) {
            Set<String> validatedKeys = new HashSet<>();
            for (String key : dto.getEnabledEventKeys()) {
                if (VALID_EVENT_KEYS.contains(key)) {
                    validatedKeys.add(key);
                } else {
                    log.warn("Invalid event key provided: {}", key);
                }
            }
            preference.setEnabledEventKeys(validatedKeys);
        } else if (dto.getEnabledEventKeys() != null) {
            // Empty set provided: clear all preferences
            preference.setEnabledEventKeys(new HashSet<>());
        }

        preference.setUpdatedAt(LocalDateTime.now());
        NotificationPreference saved = preferenceRepository.save(preference);

        log.info("Updated email notification preferences for user: {}", username);
        return ResponseEntity.ok(mapToDTO(saved));
    }

    /**
     * GET /api/notification-preferences/valid-events - Public endpoint to list valid event keys.
     */
    @GetMapping("/valid-events")
    public ResponseEntity<Set<String>> getValidEventKeys() {
        return ResponseEntity.ok(VALID_EVENT_KEYS);
    }

    /**
     * Helper: Get authenticated username from Spring Security context.
     */
    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
            return auth.getName();
        }
        return null;
    }

    /**
     * Helper: Convert NotificationPreference entity to DTO.
     */
    private NotificationPreferenceDTO mapToDTO(NotificationPreference entity) {
        NotificationPreferenceDTO dto = new NotificationPreferenceDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUser().getUsername());
        dto.setEmailEnabled(entity.getEmailEnabled());
        dto.setEnabledEventKeys(entity.getEnabledEventKeys());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
