package org.hartford.fireinsurance.dto;

public class CreatePropertyRequest {
    private String propertyType;
    private String address;
    private Double areaSqft;
    private String constructionType;

    public CreatePropertyRequest() {
    }

    public CreatePropertyRequest(String propertyType, String address,
                                Double areaSqft, String constructionType) {
        this.propertyType = propertyType;
        this.address = address;
        this.areaSqft = areaSqft;
        this.constructionType = constructionType;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getAreaSqft() {
        return areaSqft;
    }

    public void setAreaSqft(Double areaSqft) {
        this.areaSqft = areaSqft;
    }

    public String getConstructionType() {
        return constructionType;
    }

    public void setConstructionType(String constructionType) {
        this.constructionType = constructionType;
    }
}

