package org.hartford.fireinsurance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_users")
public class BlacklistedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long blacklistId;


    @Column(nullable = true)
    private String username;

    @Column(nullable = true)
    private String email;

    @Column(name = "phone_number", nullable = true)
    private String phone;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy; // Admin who added to blacklist

    @PrePersist
    void onCreate() {
        if (active == null) {
            active = true;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        // Validation: at least one identifier must be present
        if ((username == null || username.isBlank()) && (email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            throw new IllegalArgumentException("At least one of username, email, or phone must be provided for blacklist entry.");
        }
    }

    // Constructors
    public BlacklistedUser() {
    }

    public BlacklistedUser(String username, String email, String phone, String reason, String createdBy) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.reason = reason;
        this.createdBy = createdBy;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Validates that at least one identifier is present (username, email, phone).
     * Throws IllegalArgumentException if not valid.
     */
    public void validateIdentifiers() {
        if ((username == null || username.isBlank()) &&
            (email == null || email.isBlank()) &&
            (phone == null || phone.isBlank())) {
            throw new IllegalArgumentException("At least one of username, email, or phone must be provided for blacklist entry.");
        }
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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