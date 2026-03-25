package org.hartford.fireinsurance.dto;

public class SiuWorkloadResponse {
    private Long investigatorId;
    private String investigatorName;
    private Long activeCases;
    private Long completedCases;
    private Long fraudConfirmed;
    private Long cleared;
    private Double averageCompletionDays;

    // Default constructor
    public SiuWorkloadResponse() {}

    // All-args constructor
    public SiuWorkloadResponse(Long investigatorId, String investigatorName, Long activeCases,
                              Long completedCases, Long fraudConfirmed, Long cleared,
                              Double averageCompletionDays) {
        this.investigatorId = investigatorId;
        this.investigatorName = investigatorName;
        this.activeCases = activeCases;
        this.completedCases = completedCases;
        this.fraudConfirmed = fraudConfirmed;
        this.cleared = cleared;
        this.averageCompletionDays = averageCompletionDays;
    }

    // Getters and Setters
    public Long getInvestigatorId() {
        return investigatorId;
    }

    public void setInvestigatorId(Long investigatorId) {
        this.investigatorId = investigatorId;
    }

    public String getInvestigatorName() {
        return investigatorName;
    }

    public void setInvestigatorName(String investigatorName) {
        this.investigatorName = investigatorName;
    }

    public Long getActiveCases() {
        return activeCases;
    }

    public void setActiveCases(Long activeCases) {
        this.activeCases = activeCases;
    }

    public Long getCompletedCases() {
        return completedCases;
    }

    public void setCompletedCases(Long completedCases) {
        this.completedCases = completedCases;
    }

    public Long getFraudConfirmed() {
        return fraudConfirmed;
    }

    public void setFraudConfirmed(Long fraudConfirmed) {
        this.fraudConfirmed = fraudConfirmed;
    }

    public Long getCleared() {
        return cleared;
    }

    public void setCleared(Long cleared) {
        this.cleared = cleared;
    }

    public Double getAverageCompletionDays() {
        return averageCompletionDays;
    }

    public void setAverageCompletionDays(Double averageCompletionDays) {
        this.averageCompletionDays = averageCompletionDays;
    }
}