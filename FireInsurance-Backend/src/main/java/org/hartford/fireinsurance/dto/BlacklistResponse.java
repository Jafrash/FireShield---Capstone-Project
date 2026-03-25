package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.Blacklist.BlacklistType;

import java.time.LocalDateTime;

/**
 * Response DTO for blacklist entries.
 */
public class BlacklistResponse {

    private Long id;
    private BlacklistType type;
    private String value;
    private String reason;
    private LocalDateTime createdAt;
    private String createdBy;
    private Boolean isActive;

    // Constructors
    public BlacklistResponse() {
    }

    public BlacklistResponse(Long id, BlacklistType type, String value, String reason,
                            LocalDateTime createdAt, String createdBy, Boolean isActive) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.reason = reason;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BlacklistType getType() {
        return type;
    }

    public void setType(BlacklistType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
