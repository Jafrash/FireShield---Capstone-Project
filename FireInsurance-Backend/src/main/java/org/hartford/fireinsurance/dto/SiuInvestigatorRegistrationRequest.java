package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.InvestigatorSpecialization;

/**
 * DTO for SIU investigator registration requests
 */
public class SiuInvestigatorRegistrationRequest {
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String badgeNumber;
    private InvestigatorSpecialization specialization = InvestigatorSpecialization.GENERAL;
    private Integer experienceYears;

    // Constructors
    public SiuInvestigatorRegistrationRequest() {
    }

    public SiuInvestigatorRegistrationRequest(String username, String email, String password, String phoneNumber,
                                              String firstName, String lastName, String badgeNumber,
                                              InvestigatorSpecialization specialization, Integer experienceYears) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.badgeNumber = badgeNumber;
        this.specialization = specialization;
        this.experienceYears = experienceYears;
    }

    // Getters and setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}