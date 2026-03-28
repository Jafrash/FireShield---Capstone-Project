package org.hartford.fireinsurance.dto;
public class SubscribeRequest {
    private Long propertyId;
    private Long policyId;
    private Double requestedCoverage;
    private String constructionType;
    private String roofType;
    private Integer numberOfFloors;
    private String occupancyType;
    private String manufacturingProcess;
    private String hazardousGoods;
    private String previousLossHistory;
    private Boolean insuranceDeclinedBefore;
    private Double propertyValue;
    private Boolean declarationAccepted;

    public SubscribeRequest() {}
    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }
    public Double getRequestedCoverage() { return requestedCoverage; }
    public void setRequestedCoverage(Double requestedCoverage) { this.requestedCoverage = requestedCoverage; }
    public String getConstructionType() { return constructionType; }
    public void setConstructionType(String constructionType) { this.constructionType = constructionType; }
    public String getRoofType() { return roofType; }
    public void setRoofType(String roofType) { this.roofType = roofType; }
    public Integer getNumberOfFloors() { return numberOfFloors; }
    public void setNumberOfFloors(Integer numberOfFloors) { this.numberOfFloors = numberOfFloors; }
    public String getOccupancyType() { return occupancyType; }
    public void setOccupancyType(String occupancyType) { this.occupancyType = occupancyType; }
    public String getManufacturingProcess() { return manufacturingProcess; }
    public void setManufacturingProcess(String manufacturingProcess) { this.manufacturingProcess = manufacturingProcess; }
    public String getHazardousGoods() { return hazardousGoods; }
    public void setHazardousGoods(String hazardousGoods) { this.hazardousGoods = hazardousGoods; }
    public String getPreviousLossHistory() { return previousLossHistory; }
    public void setPreviousLossHistory(String previousLossHistory) { this.previousLossHistory = previousLossHistory; }
    public Boolean getInsuranceDeclinedBefore() { return insuranceDeclinedBefore; }
    public void setInsuranceDeclinedBefore(Boolean insuranceDeclinedBefore) { this.insuranceDeclinedBefore = insuranceDeclinedBefore; }
    public Double getPropertyValue() { return propertyValue; }
    public void setPropertyValue(Double propertyValue) { this.propertyValue = propertyValue; }

    public Boolean getDeclarationAccepted() { return declarationAccepted; }
    public void setDeclarationAccepted(Boolean declarationAccepted) { this.declarationAccepted = declarationAccepted; }
}
