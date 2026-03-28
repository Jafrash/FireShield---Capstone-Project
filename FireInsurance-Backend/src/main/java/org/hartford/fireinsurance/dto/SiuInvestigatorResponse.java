package org.hartford.fireinsurance.dto;

import java.time.LocalDateTime;

public class SiuInvestigatorResponse {

    private Long investigatorId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String badgeNumber;
    private String department;
    private Integer experienceYears;
    private String specialization;
    private Boolean active;
    private LocalDateTime createdAt;

    public SiuInvestigatorResponse() {
    }

    public SiuInvestigatorResponse(Long investigatorId, String username, String email, String firstName,
                                   String lastName, String phone, String badgeNumber, String department,
                                   Integer experienceYears, String specialization, Boolean active,
                                   LocalDateTime createdAt) {
        this.investigatorId = investigatorId;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.badgeNumber = badgeNumber;
        this.department = department;
        this.experienceYears = experienceYears;
        this.specialization = specialization;
        this.active = active;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getInvestigatorId() {
        return investigatorId;
    }

    public void setInvestigatorId(Long investigatorId) {
        this.investigatorId = investigatorId;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
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