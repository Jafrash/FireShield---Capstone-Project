package org.hartford.fireinsurance.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing SIU (Special Investigation Unit) investigators
 */
@Entity
@Table(name = "siu_investigators")
public class SiuInvestigator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long investigatorId;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "username")
    private String username;

    @Column(unique = true)
    private String badgeNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestigatorSpecialization specialization = InvestigatorSpecialization.GENERAL;

    @Column
    private Integer experienceYears;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public SiuInvestigator() {
    }

    public SiuInvestigator(User user, String badgeNumber, InvestigatorSpecialization specialization, Integer experienceYears) {
        this.user = user;
        this.badgeNumber = badgeNumber;
        this.specialization = specialization;
        this.experienceYears = experienceYears;
    }

    // Getters and setters
    public Long getInvestigatorId() {
        return investigatorId;
    }

    public void setInvestigatorId(Long investigatorId) {
        this.investigatorId = investigatorId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public InvestigatorSpecialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(InvestigatorSpecialization specialization) {
        this.specialization = specialization;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
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

    // Utility methods
    public String getFullName() {
        if (user != null && user.getUsername() != null) {
            // For now, return username as the full name
            // In the future, we might want to add firstName/lastName to User entity
            return user.getUsername();
        }
        return "Unknown Investigator";
    }

    public String getDisplayName() {
        return getFullName() + " (" + badgeNumber + ")";
    }

    @Override
    public String toString() {
        return "SiuInvestigator{" +
                "investigatorId=" + investigatorId +
                ", badgeNumber='" + badgeNumber + '\'' +
                ", specialization=" + specialization +
                ", experienceYears=" + experienceYears +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}