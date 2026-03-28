package org.hartford.fireinsurance.model;

import jakarta.persistence.*;

import java.util.List;

@Entity

@Table(name = "properties")

public class Property {

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long propertyId;

    @ManyToOne

    @JoinColumn(name = "customer_id")

    private Customer customer;

    private String propertyType;

    private String address;

    private Double areaSqft;

    private String constructionType;

    private Double riskScore;

    private Double latitude;

    private Double longitude;

    @Column(name = "zip_code")
    private String zipCode;

    private Integer propertyAge = 0;

    @OneToMany(mappedBy = "property")

    private List<Inspection> inspections;

    public Property() {
    }

    public Property(Long propertyId, Customer customer, String propertyType, String address, Double areaSqft,
            String constructionType, Double riskScore, List<Inspection> inspections) {
        this.propertyId = propertyId;
        this.customer = customer;
        this.propertyType = propertyType;
        this.address = address;
        this.areaSqft = areaSqft;
        this.constructionType = constructionType;
        this.riskScore = riskScore;
        this.propertyAge = 0;
        this.inspections = inspections;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public List<Inspection> getInspections() {
        return inspections;
    }

    public void setInspections(List<Inspection> inspections) {
        this.inspections = inspections;
    }

    public Integer getPropertyAge() {
        return propertyAge;
    }

    public void setPropertyAge(Integer propertyAge) {
        this.propertyAge = propertyAge;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
}