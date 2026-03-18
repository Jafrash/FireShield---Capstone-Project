package org.hartford.fireinsurance.dto;

public class SurveyorRegistrationResponse {
    private Long surveyorId;
    private String username;
    private String email;
    private String phoneNumber;
    private String licenseNumber;
    private Integer experienceYears;
    private String assignedRegion;
    private String message;

    public SurveyorRegistrationResponse() {
    }

    public SurveyorRegistrationResponse(Long surveyorId, String username, String email, String phoneNumber,
                                       String licenseNumber, Integer experienceYears, String assignedRegion, String message) {
        this.surveyorId = surveyorId;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.licenseNumber = licenseNumber;
        this.experienceYears = experienceYears;
        this.assignedRegion = assignedRegion;
        this.message = message;
    }

    public Long getSurveyorId() {
        return surveyorId;
    }

    public void setSurveyorId(Long surveyorId) {
        this.surveyorId = surveyorId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
