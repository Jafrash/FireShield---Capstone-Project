package org.hartford.fireinsurance.dto;

import java.time.LocalDateTime;

public class BlacklistResponse {

    private Long blacklistId;
    private String username;
    private String email;
    private String phoneNumber;
    private String reason;
    private Boolean active;
    private LocalDateTime createdAt;
    private String createdBy;

    // Constructors
    public BlacklistResponse() {
    }

    public BlacklistResponse(Long blacklistId, String username, String email, String phoneNumber,
                           String reason, Boolean active, LocalDateTime createdAt, String createdBy) {
        this.blacklistId = blacklistId;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.reason = reason;
        this.active = active;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public Long getBlacklistId() {
        return blacklistId;
    }

    public void setBlacklistId(Long blacklistId) {
        this.blacklistId = blacklistId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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
}