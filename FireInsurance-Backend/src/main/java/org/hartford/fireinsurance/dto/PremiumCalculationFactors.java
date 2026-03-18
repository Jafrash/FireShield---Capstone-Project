package org.hartford.fireinsurance.dto;

public class PremiumCalculationFactors {
    private Double riskMultiplier;
    private Double locationFactor;
    private Double constructionFactor;
    private Double ageFactor;

    public PremiumCalculationFactors() {
    }

    public PremiumCalculationFactors(Double riskMultiplier, Double locationFactor, 
                                    Double constructionFactor, Double ageFactor) {
        this.riskMultiplier = riskMultiplier;
        this.locationFactor = locationFactor;
        this.constructionFactor = constructionFactor;
        this.ageFactor = ageFactor;
    }

    public Double getRiskMultiplier() {
        return riskMultiplier;
    }

    public void setRiskMultiplier(Double riskMultiplier) {
        this.riskMultiplier = riskMultiplier;
    }

    public Double getLocationFactor() {
        return locationFactor;
    }

    public void setLocationFactor(Double locationFactor) {
        this.locationFactor = locationFactor;
    }

    public Double getConstructionFactor() {
        return constructionFactor;
    }

    public void setConstructionFactor(Double constructionFactor) {
        this.constructionFactor = constructionFactor;
    }

    public Double getAgeFactor() {
        return ageFactor;
    }

    public void setAgeFactor(Double ageFactor) {
        this.ageFactor = ageFactor;
    }
}
