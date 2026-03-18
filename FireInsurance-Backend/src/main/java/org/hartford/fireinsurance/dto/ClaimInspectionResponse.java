package org.hartford.fireinsurance.dto;
import org.hartford.fireinsurance.model.ClaimInspection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
public class ClaimInspectionResponse {
    private Long inspectionId;
    private Long claimId;
    private String surveyorName;
    private LocalDateTime inspectionDate;
    private Double estimatedLoss;
    private ClaimInspection.ClaimInspectionStatus status;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Double requestedClaimAmount;
    private String claimDescription;
    private List<InspectionDocumentSummary> customerDocuments = new ArrayList<>();
    private String causeOfFire;
    private Double salvageValue;
    private Double fireBrigadeExpenses;
    private String otherInsuranceDetails;
    private Boolean underInsuranceDetected;
    private Double recommendedSettlement;
    public ClaimInspectionResponse() {}
    public ClaimInspectionResponse(Long inspectionId, Long claimId, String surveyorName,
                                  LocalDateTime inspectionDate, Double estimatedLoss,
                                  ClaimInspection.ClaimInspectionStatus status) {
        this.inspectionId = inspectionId;
        this.claimId = claimId;
        this.surveyorName = surveyorName;
        this.inspectionDate = inspectionDate;
        this.estimatedLoss = estimatedLoss;
        this.status = status;
    }
    public Long getInspectionId() { return inspectionId; }
    public void setInspectionId(Long inspectionId) { this.inspectionId = inspectionId; }
    public Long getClaimId() { return claimId; }
    public void setClaimId(Long claimId) { this.claimId = claimId; }
    public String getSurveyorName() { return surveyorName; }
    public void setSurveyorName(String surveyorName) { this.surveyorName = surveyorName; }
    public LocalDateTime getInspectionDate() { return inspectionDate; }
    public void setInspectionDate(LocalDateTime inspectionDate) { this.inspectionDate = inspectionDate; }
    public Double getEstimatedLoss() { return estimatedLoss; }
    public void setEstimatedLoss(Double estimatedLoss) { this.estimatedLoss = estimatedLoss; }
    public ClaimInspection.ClaimInspectionStatus getStatus() { return status; }
    public void setStatus(ClaimInspection.ClaimInspectionStatus status) { this.status = status; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public Double getRequestedClaimAmount() { return requestedClaimAmount; }
    public void setRequestedClaimAmount(Double requestedClaimAmount) { this.requestedClaimAmount = requestedClaimAmount; }
    public String getClaimDescription() { return claimDescription; }
    public void setClaimDescription(String claimDescription) { this.claimDescription = claimDescription; }
    public List<InspectionDocumentSummary> getCustomerDocuments() { return customerDocuments; }
    public void setCustomerDocuments(List<InspectionDocumentSummary> customerDocuments) { this.customerDocuments = customerDocuments; }

    public String getCauseOfFire() { return causeOfFire; }
    public void setCauseOfFire(String causeOfFire) { this.causeOfFire = causeOfFire; }
    public Double getSalvageValue() { return salvageValue; }
    public void setSalvageValue(Double salvageValue) { this.salvageValue = salvageValue; }
    public Double getFireBrigadeExpenses() { return fireBrigadeExpenses; }
    public void setFireBrigadeExpenses(Double fireBrigadeExpenses) { this.fireBrigadeExpenses = fireBrigadeExpenses; }
    public String getOtherInsuranceDetails() { return otherInsuranceDetails; }
    public void setOtherInsuranceDetails(String otherInsuranceDetails) { this.otherInsuranceDetails = otherInsuranceDetails; }
    public Boolean getUnderInsuranceDetected() { return underInsuranceDetected; }
    public void setUnderInsuranceDetected(Boolean underInsuranceDetected) { this.underInsuranceDetected = underInsuranceDetected; }
    public Double getRecommendedSettlement() { return recommendedSettlement; }
    public void setRecommendedSettlement(Double recommendedSettlement) { this.recommendedSettlement = recommendedSettlement; }
}
