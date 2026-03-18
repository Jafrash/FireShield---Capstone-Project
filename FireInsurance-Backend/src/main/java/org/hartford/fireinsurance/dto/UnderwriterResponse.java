package org.hartford.fireinsurance.dto;

import java.time.LocalDateTime;

public class UnderwriterResponse {

    private Long underwriterId;
    private String username;
    private String email;
    private String phoneNumber;
    private String department;
    private String region;
    private Integer experienceYears;
    private Boolean active;
    private LocalDateTime createdAt;

    public UnderwriterResponse() {
    }

    public UnderwriterResponse(Long underwriterId, String username, String email, String phoneNumber,
                               String department, String region, Integer experienceYears,
                               Boolean active, LocalDateTime createdAt) {
        this.underwriterId = underwriterId;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.department = department;
        this.region = region;
        this.experienceYears = experienceYears;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getUnderwriterId() {
        return underwriterId;
    }

    public void setUnderwriterId(Long underwriterId) {
        this.underwriterId = underwriterId;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
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
