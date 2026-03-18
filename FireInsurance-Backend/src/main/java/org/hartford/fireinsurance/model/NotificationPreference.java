package org.hartford.fireinsurance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_preference_events", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "event_key")
    private Set<String> enabledEventKeys = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public NotificationPreference(User user) {
        this.user = user;
        this.emailEnabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.enabledEventKeys = getDefaultEventKeys();
    }

    public static Set<String> getDefaultEventKeys() {
        Set<String> defaults = new HashSet<>();
        // Default enabled events for all users
        defaults.add("POLICY_SUBMITTED");
        defaults.add("CLAIM_STATUS_CHANGE");
        defaults.add("INSPECTION_ASSIGNED");
        defaults.add("INSPECTION_COMPLETED");
        defaults.add("POLICY_APPROVAL");
        defaults.add("POLICY_REJECTION");
        defaults.add("POLICY_RENEWAL_REMINDER");
        return defaults;
    }

    public void setDefaultsForRole(String role) {
        if (role == null) {
            return;
        }
        this.enabledEventKeys = getDefaultEventKeys();
        // Can customize per-role in future
    }
}
