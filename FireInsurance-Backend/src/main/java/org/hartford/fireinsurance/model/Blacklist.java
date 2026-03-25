package org.hartford.fireinsurance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a blacklist entry for fraud detection.
 * Entries can be of type USER, PHONE, ADDRESS, or EMAIL.
 * Used to flag suspicious entities during claim processing.
 */
@Entity
@Table(name = "blacklist")
public class Blacklist {

    // Blacklist entry type enumeration
    public enum BlacklistType {
        USER,       // Blacklisted username/user ID
        PHONE,      // Blacklisted phone number
        ADDRESS,    // Blacklisted property address
        EMAIL       // Blacklisted email address
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlacklistType type;

    @Column(name = "blacklist_value", nullable = false)
    private String value;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Constructors
    public Blacklist() {
    }

    public Blacklist(BlacklistType type, String value, String reason, String createdBy) {
        this.type = type;
        this.value = value;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
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
