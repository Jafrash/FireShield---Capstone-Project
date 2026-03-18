package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.PolicySubscription;
import java.time.LocalDate;

public class SubscriptionResponse {

    private Long subscriptionId;
    private Long propertyId;
    private String policyName;
    private LocalDate startDate;
    private LocalDate endDate;
    private PolicySubscription.SubscriptionStatus status;
    private Double premiumAmount;
    
    // NEW: Risk-based premium calculation fields
    private Double basePremiumAmount;
    private Double riskScore;
    private Double riskMultiplier;
    private Long inspectionId;
    
    // RENEWAL WORKFLOW FIELDS
    private Boolean renewalEligible;
    private Long previousSubscriptionId;
    private Integer renewalCount;
    private Long daysRemaining;
    
    // NCB FIELDS
    private Integer claimFreeYears;
    private Double ncbDiscount;
    
    // COVERAGE REQUESTED
    private Double requestedCoverage;
    
    // UNDERWRITER ASSIGNMENT
    private Long underwriterId;

    // PAYMENT + DOCUMENTS
    private Boolean paymentReceived;
    private String coverNoteFileName;
    private String policyDocumentFileName;

    // PROPOSAL / UNDERWRITING FIELDS
    private String constructionType;
    private String roofType;
    private Integer numberOfFloors;
    private String occupancyType;
    private String manufacturingProcess;
    private String hazardousGoods;
    private String previousLossHistory;
    private Boolean insuranceDeclinedBefore;
    private Double propertyValue;

    public SubscriptionResponse() {}

    public SubscriptionResponse(Long subscriptionId, Long propertyId, String policyName,
                               LocalDate startDate, LocalDate endDate,
                               PolicySubscription.SubscriptionStatus status, 
                               Double premiumAmount, Double basePremiumAmount,
                               Double riskScore, Double riskMultiplier, Long inspectionId,
                               Long underwriterId, Double requestedCoverage) {
        this.subscriptionId = subscriptionId;
        this.propertyId = propertyId;
        this.policyName = policyName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.premiumAmount = premiumAmount;
        this.basePremiumAmount = basePremiumAmount;
        this.riskScore = riskScore;
        this.riskMultiplier = riskMultiplier;
        this.inspectionId = inspectionId;
        this.underwriterId = underwriterId;
        this.requestedCoverage = requestedCoverage;
        this.renewalEligible = false;
        this.renewalCount = 0;
        this.claimFreeYears = 0;
        this.ncbDiscount = 0.0;
    }

    // Getters and Setters

    public Long getSubscriptionId() { 
        return subscriptionId; 
    }

    public void setSubscriptionId(Long subscriptionId) { 
        this.subscriptionId = subscriptionId; 
    }

    public Long getPropertyId() { 
        return propertyId; 
    }

    public void setPropertyId(Long propertyId) { 
        this.propertyId = propertyId; 
    }

    public String getPolicyName() { 
        return policyName; 
    }

    public void setPolicyName(String policyName) { 
        this.policyName = policyName; 
    }

    public LocalDate getStartDate() { 
        return startDate; 
    }

    public void setStartDate(LocalDate startDate) { 
        this.startDate = startDate; 
    }

    public LocalDate getEndDate() { 
        return endDate; 
    }

    public void setEndDate(LocalDate endDate) { 
        this.endDate = endDate; 
    }

    public PolicySubscription.SubscriptionStatus getStatus() { 
        return status; 
    }

    public void setStatus(PolicySubscription.SubscriptionStatus status) { 
        this.status = status; 
    }

    public Double getPremiumAmount() { 
        return premiumAmount; 
    }

    public void setPremiumAmount(Double premiumAmount) { 
        this.premiumAmount = premiumAmount; 
    }

    public Double getBasePremiumAmount() { 
        return basePremiumAmount; 
    }

    public void setBasePremiumAmount(Double basePremiumAmount) { 
        this.basePremiumAmount = basePremiumAmount; 
    }

    public Double getRiskScore() { 
        return riskScore; 
    }

    public void setRiskScore(Double riskScore) { 
        this.riskScore = riskScore; 
    }

    public Double getRiskMultiplier() { 
        return riskMultiplier; 
    }

    public void setRiskMultiplier(Double riskMultiplier) { 
        this.riskMultiplier = riskMultiplier; 
    }

    public Long getInspectionId() { 
        return inspectionId; 
    }

    public void setInspectionId(Long inspectionId) { 
        this.inspectionId = inspectionId; 
    }
    
    // RENEWAL WORKFLOW GETTERS AND SETTERS
    
    public Boolean getRenewalEligible() {
        return renewalEligible;
    }
    
    public void setRenewalEligible(Boolean renewalEligible) {
        this.renewalEligible = renewalEligible;
    }
    
    public Long getPreviousSubscriptionId() {
        return previousSubscriptionId;
    }
    
    public void setPreviousSubscriptionId(Long previousSubscriptionId) {
        this.previousSubscriptionId = previousSubscriptionId;
    }
    
    public Integer getRenewalCount() {
        return renewalCount;
    }
    
    public void setRenewalCount(Integer renewalCount) {
        this.renewalCount = renewalCount;
    }
    
    public Long getDaysRemaining() {
        return daysRemaining;
    }
    
    public void setDaysRemaining(Long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }
    
    // NCB GETTERS AND SETTERS
    
    public Integer getClaimFreeYears() {
        return claimFreeYears;
    }
    
    public void setClaimFreeYears(Integer claimFreeYears) {
        this.claimFreeYears = claimFreeYears;
    }
    
    public Double getNcbDiscount() {
        return ncbDiscount;
    }
    
    public void setNcbDiscount(Double ncbDiscount) {
        this.ncbDiscount = ncbDiscount;
    }

    public Long getUnderwriterId() {
        return underwriterId;
    }

    public void setUnderwriterId(Long underwriterId) {
        this.underwriterId = underwriterId;
    }

    public Double getRequestedCoverage() {
        return requestedCoverage;
    }

    public void setRequestedCoverage(Double requestedCoverage) {
        this.requestedCoverage = requestedCoverage;
    }

    public Boolean getPaymentReceived() {
        return paymentReceived;
    }

    public void setPaymentReceived(Boolean paymentReceived) {
        this.paymentReceived = paymentReceived;
    }

    public String getCoverNoteFileName() {
        return coverNoteFileName;
    }

    public void setCoverNoteFileName(String coverNoteFileName) {
        this.coverNoteFileName = coverNoteFileName;
    }

    public String getPolicyDocumentFileName() {
        return policyDocumentFileName;
    }

    public void setPolicyDocumentFileName(String policyDocumentFileName) {
        this.policyDocumentFileName = policyDocumentFileName;
    }

    public String getConstructionType() {
        return constructionType;
    }

    public void setConstructionType(String constructionType) {
        this.constructionType = constructionType;
    }

    public String getRoofType() {
        return roofType;
    }

    public void setRoofType(String roofType) {
        this.roofType = roofType;
    }

    public Integer getNumberOfFloors() {
        return numberOfFloors;
    }

    public void setNumberOfFloors(Integer numberOfFloors) {
        this.numberOfFloors = numberOfFloors;
    }

    public String getOccupancyType() {
        return occupancyType;
    }

    public void setOccupancyType(String occupancyType) {
        this.occupancyType = occupancyType;
    }

    public String getManufacturingProcess() {
        return manufacturingProcess;
    }

    public void setManufacturingProcess(String manufacturingProcess) {
        this.manufacturingProcess = manufacturingProcess;
    }

    public String getHazardousGoods() {
        return hazardousGoods;
    }

    public void setHazardousGoods(String hazardousGoods) {
        this.hazardousGoods = hazardousGoods;
    }

    public String getPreviousLossHistory() {
        return previousLossHistory;
    }

    public void setPreviousLossHistory(String previousLossHistory) {
        this.previousLossHistory = previousLossHistory;
    }

    public Boolean getInsuranceDeclinedBefore() {
        return insuranceDeclinedBefore;
    }

    public void setInsuranceDeclinedBefore(Boolean insuranceDeclinedBefore) {
        this.insuranceDeclinedBefore = insuranceDeclinedBefore;
    }

    public Double getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(Double propertyValue) {
        this.propertyValue = propertyValue;
    }
}
