package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.model.Inspection;
import org.hartford.fireinsurance.model.Policy;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.springframework.stereotype.Service;

@Service
public class PremiumCalculationService {

    public double calculatePremium(PolicySubscription subscription, Double riskScore, Inspection inspection) {
        Policy policy = subscription.getPolicy();
        double approvedCoverage = getApprovedCoverage(subscription);
        double maxCoverage = policy != null && policy.getMaxCoverageAmount() != null && policy.getMaxCoverageAmount() > 0
                ? policy.getMaxCoverageAmount() : approvedCoverage;

        double baseRate = (policy != null && policy.getBasePremium() != null && maxCoverage > 0)
                ? (policy.getBasePremium() / maxCoverage) : 0.003;

        double riskFactor = computeRiskFactor(subscription, riskScore, inspection);
        return round2(approvedCoverage * baseRate * riskFactor);
    }

    public double getRiskMultiplier(double riskScore) {
        if (riskScore <= 2) return 0.8;
        else if (riskScore <= 4) return 1.0;
        else if (riskScore <= 6) return 1.2;
        else if (riskScore <= 8) return 1.5;
        else return 2.0;
    }

    public double computeRiskFactor(PolicySubscription subscription, Double riskScore, Inspection inspection) {
        double factor = 1.0; // Base baseline

        // 1. Initial manual score multiplier (optional weighting)
        if (riskScore != null) {
            factor = getRiskMultiplier(riskScore);
        }

        // 2. Inspection-Verified COPE Factors (High Fidelity)
        if (inspection != null) {
            // Construction Type Weighting
            if (inspection.getConstructionType() != null) {
                switch (inspection.getConstructionType().toUpperCase()) {
                    case "FRAME" -> factor *= 1.20;
                    case "JOISTED_MASONRY" -> factor *= 1.10;
                    case "NON_COMBUSTIBLE" -> factor *= 0.95;
                    case "FIRE_RESISTIVE" -> factor *= 0.85;
                }
            }

            // Roofing Multiplier
            if (inspection.getRoofType() != null) {
                switch (inspection.getRoofType().toUpperCase()) {
                    case "WOOD" -> factor *= 1.15;
                    case "ASPHALT" -> factor *= 1.05;
                    case "TILE" -> factor *= 0.98;
                    case "CONCRETE" -> factor *= 0.95;
                }
            }

            // Occupancy Verification
            if (inspection.getOccupancyType() != null) {
                switch (inspection.getOccupancyType().toUpperCase()) {
                    case "INDUSTRIAL" -> factor *= 1.25;
                    case "WAREHOUSE" -> factor *= 1.15;
                    case "COMMERCIAL" -> factor *= 1.10;
                    // Residential is default 1.0
                }
            }

            // Protection & Hazards (Additive)
            if ("FAIL".equalsIgnoreCase(inspection.getElectricalAuditStatus())) {
                factor += 0.30; // Critical surcharge
            } else if ("PASS".equalsIgnoreCase(inspection.getElectricalAuditStatus())) {
                factor -= 0.05; // Safety discount
            }

            if (Boolean.TRUE.equals(inspection.getHazardousMaterialsPresent())) {
                factor += 0.20;
            }

            if (Boolean.TRUE.equals(inspection.getSprinklerSystem())) {
                factor -= 0.10;
            }

            if (Boolean.TRUE.equals(inspection.getFireSafetyAvailable())) {
                factor -= 0.05;
            }

            // Exposure (Proximity)
            if (inspection.getAdjacentBuildingDistance() != null) {
                if (inspection.getAdjacentBuildingDistance() < 5) factor += 0.15;
                else if (inspection.getAdjacentBuildingDistance() > 25) factor -= 0.05;
            }
        } else {
            // Fallback for missing inspection: basic subscription traits
            if ("INDUSTRIAL".equalsIgnoreCase(subscription.getOccupancyType())) {
                factor *= 1.15;
            }
            if (subscription.getHazardousGoods() != null && !subscription.getHazardousGoods().isBlank()) {
                factor += 0.15;
            }
        }

        // 3. Historical Data Risk
        if (Boolean.TRUE.equals(subscription.getInsuranceDeclinedBefore())) {
            factor += 0.15;
        }

        // Technical Boundaries
        if (factor < 0.60) factor = 0.60;
        if (factor > 4.00) factor = 4.00;
        
        return factor;
    }

    public double getApprovedCoverage(PolicySubscription subscription) {
        Double requested = subscription.getRequestedCoverage();
        Double maxCoverage = subscription.getPolicy() != null ? subscription.getPolicy().getMaxCoverageAmount() : null;
        if (requested == null || requested <= 0) {
            return maxCoverage != null ? maxCoverage : 0.0;
        }
        if (maxCoverage != null && maxCoverage > 0) {
            return Math.min(requested, maxCoverage);
        }
        return requested;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}