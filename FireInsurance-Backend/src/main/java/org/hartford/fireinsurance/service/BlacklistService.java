package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.BlacklistRequest;
import org.hartford.fireinsurance.dto.BlacklistResponse;
import org.hartford.fireinsurance.model.Blacklist;
import org.hartford.fireinsurance.model.Blacklist.BlacklistType;
import org.hartford.fireinsurance.repository.BlacklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing the fraud detection blacklist.
 * Handles CRUD operations for blacklist entries.
 */
@Service
@Transactional
public class BlacklistService {

    private static final Logger log = LoggerFactory.getLogger(BlacklistService.class);

    private final BlacklistRepository blacklistRepository;

    public BlacklistService(BlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    /**
     * Add a new entry to the blacklist.
     */
    public BlacklistResponse addToBlacklist(BlacklistRequest request, String createdBy) {
        log.info("Adding to blacklist: type={}, value={}, by={}", request.getType(), request.getValue(), createdBy);

        // Check if already exists
        if (blacklistRepository.existsByTypeAndValueAndIsActiveTrue(request.getType(), request.getValue())) {
            throw new RuntimeException("Entry already exists in blacklist: " + request.getType() + " - " + request.getValue());
        }

        Blacklist blacklist = new Blacklist();
        blacklist.setType(request.getType());
        blacklist.setValue(request.getValue());
        blacklist.setReason(request.getReason());
        blacklist.setCreatedAt(LocalDateTime.now());
        blacklist.setCreatedBy(createdBy);
        blacklist.setIsActive(true);

        Blacklist saved = blacklistRepository.save(blacklist);
        log.info("Blacklist entry created with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    /**
     * Get all active blacklist entries.
     */
    public List<BlacklistResponse> getAllBlacklist() {
        return blacklistRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get blacklist entries by type.
     */
    public List<BlacklistResponse> getBlacklistByType(BlacklistType type) {
        return blacklistRepository.findByTypeAndIsActiveTrue(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Soft-delete a blacklist entry (mark as inactive).
     */
    public void removeFromBlacklist(Long id) {
        Blacklist blacklist = blacklistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blacklist entry not found with ID: " + id));

        log.info("Removing blacklist entry ID: {} (type={}, value={})", id, blacklist.getType(), blacklist.getValue());
        blacklist.setIsActive(false);
        blacklistRepository.save(blacklist);
    }

    /**
     * Check if a value is blacklisted.
     */
    public boolean isBlacklisted(BlacklistType type, String value) {
        return blacklistRepository.existsByTypeAndValueAndIsActiveTrue(type, value);
    }

    /**
     * Check if a value is blacklisted (case-insensitive).
     */
    public boolean isBlacklistedIgnoreCase(BlacklistType type, String value) {
        return blacklistRepository.existsActiveByTypeAndValueIgnoreCase(type, value);
    }

    /**
     * Map entity to response DTO.
     */
    private BlacklistResponse mapToResponse(Blacklist blacklist) {
        return new BlacklistResponse(
                blacklist.getId(),
                blacklist.getType(),
                blacklist.getValue(),
                blacklist.getReason(),
                blacklist.getCreatedAt(),
                blacklist.getCreatedBy(),
                blacklist.getIsActive()
        );
    }
}
