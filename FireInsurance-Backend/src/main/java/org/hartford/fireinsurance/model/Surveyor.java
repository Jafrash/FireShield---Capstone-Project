package org.hartford.fireinsurance.model;


import jakarta.persistence.*;

@Entity
@Table(name = "surveyors")
public class Surveyor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long surveyorId;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String licenseNumber;

    private Integer experienceYears;

    private String assignedRegion;

    public Surveyor() {
    }

    public Surveyor(Long surveyorId, User user, String licenseNumber, Integer experienceYears, String assignedRegion) {
        this.surveyorId = surveyorId;
        this.user = user;
        this.licenseNumber = licenseNumber;
        this.experienceYears = experienceYears;
        this.assignedRegion = assignedRegion;
    }

    public Long getSurveyorId() {
        return surveyorId;
    }

    public void setSurveyorId(Long surveyorId) {
        this.surveyorId = surveyorId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
