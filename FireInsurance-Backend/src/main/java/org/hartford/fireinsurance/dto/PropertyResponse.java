package org.hartford.fireinsurance.dto;

public class PropertyResponse {
    private Long propertyId;
    private String propertyType;
    private String address;
    private Double areaSqft;
    private String constructionType;
    private Double riskScore;
    private Double latitude;
    private Double longitude;
    private String zipCode;

    // Added constructor to match usage in PropertyController
    public PropertyResponse(Long propertyId, String propertyType, String address, Double areaSqft, String constructionType, Double riskScore) {
        this.propertyId = propertyId;
        this.propertyType = propertyType;
        this.address = address;
        this.areaSqft = areaSqft;
        this.constructionType = constructionType;
        this.riskScore = riskScore;
    }

    public PropertyResponse() {}

    public Long getPropertyId() { return propertyId; }
    public void setPropertyId(Long propertyId) { this.propertyId = propertyId; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getAreaSqft() { return areaSqft; }
    public void setAreaSqft(Double areaSqft) { this.areaSqft = areaSqft; }

    public String getConstructionType() { return constructionType; }
    public void setConstructionType(String constructionType) { this.constructionType = constructionType; }

    public Double getRiskScore() { return riskScore; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
}
