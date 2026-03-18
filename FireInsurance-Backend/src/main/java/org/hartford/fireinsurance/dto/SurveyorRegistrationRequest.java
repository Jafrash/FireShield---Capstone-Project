package org.hartford.fireinsurance.dto;

public class SurveyorRegistrationRequest {
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
    private String licenseNumber;
    private Integer experienceYears;
    private String assignedRegion;

    public SurveyorRegistrationRequest() {
    }

    public SurveyorRegistrationRequest(String username, String email, String password, String phoneNumber,
                                      String licenseNumber, Integer experienceYears, String assignedRegion) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.licenseNumber = licenseNumber;
        this.experienceYears = experienceYears;
        this.assignedRegion = assignedRegion;
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

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getAssignedRegion() {
        return assignedRegion;
    }

    public void setAssignedRegion(String assignedRegion) {
        this.assignedRegion = assignedRegion;
    }
}
