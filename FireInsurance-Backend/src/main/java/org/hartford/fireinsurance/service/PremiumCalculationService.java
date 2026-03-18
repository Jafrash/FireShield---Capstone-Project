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
        double score = riskScore != null ? riskScore : 5.0;
        double factor = getRiskMultiplier(score);

        if (subscription.getHazardousGoods() != null && !subscription.getHazardousGoods().isBlank()) {
            factor += 0.15;
        }
        if ("INDUSTRIAL".equalsIgnoreCase(subscription.getOccupancyType())) {
            factor += 0.10;
        }
        if (Boolean.TRUE.equals(subscription.getInsuranceDeclinedBefore())) {
            factor += 0.10;
        }

        if (inspection != null) {
            if (Boolean.TRUE.equals(inspection.getFireSafetyAvailable())) {
                factor -= 0.05;
            }
            if (Boolean.TRUE.equals(inspection.getSprinklerSystem())) {
                factor -= 0.07;
            }
            if (Boolean.TRUE.equals(inspection.getFireExtinguishers())) {
                factor -= 0.03;
            }
            if (inspection.getConstructionRisk() != null) {
                factor += inspection.getConstructionRisk() * 0.05;
            }
            if (inspection.getHazardRisk() != null) {
                factor += inspection.getHazardRisk() * 0.07;
            }
        }

        if (factor < 0.70) factor = 0.70;
        if (factor > 3.00) factor = 3.00;
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