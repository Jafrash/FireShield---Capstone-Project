package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.InvestigatorSpecialization;
import org.hartford.fireinsurance.model.SiuInvestigator;

import java.time.LocalDateTime;

/**
 * DTO for SIU investigator responses
 */
public class SiuInvestigatorResponse {
    private Long investigatorId;
    private Long userId;
    private String username;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String badgeNumber;
    private InvestigatorSpecialization specialization;
    private Integer experienceYears;
    private Boolean active;
    private LocalDateTime createdAt;

    // Default constructor
    public SiuInvestigatorResponse() {
    }

    // Constructor from SiuInvestigator entity
    public SiuInvestigatorResponse(SiuInvestigator investigator) {
        this.investigatorId = investigator.getInvestigatorId();
        this.userId = investigator.getUser().getId();

        // Use username from investigator entity directly, fallback to User entity
        this.username = investigator.getUsername() != null ?
                        investigator.getUsername() :
                        investigator.getUser().getUsername();

        this.email = investigator.getUser().getEmail();
        this.phoneNumber = investigator.getUser().getPhoneNumber();

        // Since User entity doesn't have firstName/lastName, parse from username or leave empty
        String usernameForParsing = this.username != null ? this.username : "";
        String[] nameParts = usernameForParsing.split(" ", 2);
        this.firstName = nameParts.length > 0 ? nameParts[0] : "";
        this.lastName = nameParts.length > 1 ? nameParts[1] : "";

        this.badgeNumber = investigator.getBadgeNumber();
        this.specialization = investigator.getSpecialization();
        this.experienceYears = investigator.getExperienceYears();
        this.active = investigator.getActive();
        this.createdAt = investigator.getCreatedAt();
    }

    // Getters and setters
    public Long getInvestigatorId() {
        return investigatorId;
    }

    public void setInvestigatorId(Long investigatorId) {
        this.investigatorId = investigatorId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
}