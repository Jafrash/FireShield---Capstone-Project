package org.hartford.fireinsurance.dto;

public class UpdateSurveyorRequest {
    private String phoneNumber;
    private String licenseNumber;
    private Integer experienceYears;
    private String assignedRegion;

    public UpdateSurveyorRequest() {
    }

    public UpdateSurveyorRequest(String phoneNumber, String licenseNumber,
                                Integer experienceYears, String assignedRegion) {
        this.phoneNumber = phoneNumber;
        this.licenseNumber = licenseNumber;
        this.experienceYears = experienceYears;
        this.assignedRegion = assignedRegion;
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

