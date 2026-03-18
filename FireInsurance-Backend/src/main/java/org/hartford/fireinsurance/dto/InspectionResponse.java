package org.hartford.fireinsurance.dto;
import org.hartford.fireinsurance.model.Inspection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
public class InspectionResponse {
    private Long inspectionId;
    private Long propertyId;
    private String surveyorName;
    private LocalDateTime inspectionDate;
    private Double assessedRiskScore;
    private Inspection.InspectionStatus status;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String propertyAddress;
    private String propertyType;
    private List<InspectionDocumentSummary> customerDocuments = new ArrayList<>();
    private Boolean fireSafetyAvailable;
    private Boolean sprinklerSystem;
    private Boolean fireExtinguishers;
    private Double distanceFromFireStation;
    private Double constructionRisk;
    private Double hazardRisk;
    private Double recommendedCoverage;
    private Double recommendedPremium;
    public InspectionResponse() {}
    public InspectionResponse(Long inspectionId, Long propertyId, String surveyorName,
                             LocalDateTime inspectionDate, Double assessedRiskScore,
                             Inspection.InspectionStatus status) {
        this.inspectionId = inspectionId;
        this.propertyId = propertyId;
        this.surveyorName = surveyorName;
        this.inspectionDate = inspectionDate;
        this.assessedRiskScore = assessedRiskScore;
        this.status = status;
    }
    public Long getInspectionId() { return inspectionId; }
    public void setInspectionId(Long inspectionId) { this.inspectionId = inspectionId; }
    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }
    public String getSurveyorName() { return surveyorName; }
    public void setSurveyorName(String surveyorName) { this.surveyorName = surveyorName; }
    public LocalDateTime getInspectionDate() { return inspectionDate; }
    public void setInspectionDate(LocalDateTime inspectionDate) { this.inspectionDate = inspectionDate; }
    public Double getAssessedRiskScore() { return assessedRiskScore; }
    public void setAssessedRiskScore(Double assessedRiskScore) { this.assessedRiskScore = assessedRiskScore; }
    public Inspection.InspectionStatus getStatus() { return status; }
    public void setStatus(Inspection.InspectionStatus status) { this.status = status; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public List<InspectionDocumentSummary> getCustomerDocuments() { return customerDocuments; }
    public void setCustomerDocuments(List<InspectionDocumentSummary> customerDocuments) { this.customerDocuments = customerDocuments; }

    public Boolean getFireSafetyAvailable() { return fireSafetyAvailable; }
    public void setFireSafetyAvailable(Boolean fireSafetyAvailable) { this.fireSafetyAvailable = fireSafetyAvailable; }
    public Boolean getSprinklerSystem() { return sprinklerSystem; }
    public void setSprinklerSystem(Boolean sprinklerSystem) { this.sprinklerSystem = sprinklerSystem; }
    public Boolean getFireExtinguishers() { return fireExtinguishers; }
    public void setFireExtinguishers(Boolean fireExtinguishers) { this.fireExtinguishers = fireExtinguishers; }
    public Double getDistanceFromFireStation() { return distanceFromFireStation; }
    public void setDistanceFromFireStation(Double distanceFromFireStation) { this.distanceFromFireStation = distanceFromFireStation; }
    public Double getConstructionRisk() { return constructionRisk; }
    public void setConstructionRisk(Double constructionRisk) { this.constructionRisk = constructionRisk; }
    public Double getHazardRisk() { return hazardRisk; }
    public void setHazardRisk(Double hazardRisk) { this.hazardRisk = hazardRisk; }
    public Double getRecommendedCoverage() { return recommendedCoverage; }
    public void setRecommendedCoverage(Double recommendedCoverage) { this.recommendedCoverage = recommendedCoverage; }
    public Double getRecommendedPremium() { return recommendedPremium; }
    public void setRecommendedPremium(Double recommendedPremium) { this.recommendedPremium = recommendedPremium; }
}
