package org.hartford.fireinsurance.dto;
import java.time.LocalDate;
public class CreateClaimRequest {
    private Long subscriptionId;
    private String description;
    private Double claimAmount;
    private LocalDate incidentDate;
    private String causeOfFire;
    private String firNumber;
    private String fireBrigadeReportNumber;
    private String salvageDetails;
    private String lossType;
    private String policeStation;
    private String contactPhoneNumber;
    private String witnessDetails;

    public CreateClaimRequest() {}

    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getClaimAmount() { return claimAmount; }
    public void setClaimAmount(Double claimAmount) { this.claimAmount = claimAmount; }
    public LocalDate getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDate incidentDate) { this.incidentDate = incidentDate; }
    public String getCauseOfFire() { return causeOfFire; }
    public void setCauseOfFire(String causeOfFire) { this.causeOfFire = causeOfFire; }
    public String getFirNumber() { return firNumber; }
    public void setFirNumber(String firNumber) { this.firNumber = firNumber; }
    public String getFireBrigadeReportNumber() { return fireBrigadeReportNumber; }
    public void setFireBrigadeReportNumber(String fireBrigadeReportNumber) { this.fireBrigadeReportNumber = fireBrigadeReportNumber; }
    public String getSalvageDetails() { return salvageDetails; }
    public void setSalvageDetails(String salvageDetails) { this.salvageDetails = salvageDetails; }

    public String getLossType() { return lossType; }
    public void setLossType(String lossType) { this.lossType = lossType; }
    public String getPoliceStation() { return policeStation; }
    public void setPoliceStation(String policeStation) { this.policeStation = policeStation; }
    public String getContactPhoneNumber() { return contactPhoneNumber; }
    public void setContactPhoneNumber(String contactPhoneNumber) { this.contactPhoneNumber = contactPhoneNumber; }
    public String getWitnessDetails() { return witnessDetails; }
    public void setWitnessDetails(String witnessDetails) { this.witnessDetails = witnessDetails; }
}
