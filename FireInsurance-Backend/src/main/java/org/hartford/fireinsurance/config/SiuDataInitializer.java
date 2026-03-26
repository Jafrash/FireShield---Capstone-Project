package org.hartford.fireinsurance.config;

import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Initializer to add fraud scores to existing claims for SIU testing.
 * This runs after the AdminInitializer to ensure users exist.
 */
@Component
@Order(2) // Run after AdminInitializer
public class SiuDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SiuDataInitializer.class);

    private final ClaimRepository claimRepository;
    private final Random random = new Random(12345); // Fixed seed for consistent results

    public SiuDataInitializer(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    @Override
    public void run(String... args) {
        try {
            // Update existing claims with fraud scores if they don't have them
            List<Claim> claims = claimRepository.findAll();

            if (claims.isEmpty()) {
                log.info("No existing claims found - SIU data initialization skipped");
                return;
            }

            int updatedCount = 0;
            for (Claim claim : claims) {
                if (claim.getFraudScore() == null || claim.getFraudScore() == 0.0) {
                    // Generate realistic fraud scores with some variety
                    double fraudScore = generateRealisticFraudScore(claim);
                    claim.setFraudScore(fraudScore);
                    // Fix: If status is SURVEY_ASSIGNED, set to SURVEYOR_ASSIGNED
                    if (claim.getStatus() != null && claim.getStatus().name().equals("SURVEY_ASSIGNED")) {
                        claim.setStatus(Claim.ClaimStatus.SURVEYOR_ASSIGNED);
                    }
                    // Fallback: If status is not a valid enum, set to CREATED
                    try {
                        Claim.ClaimStatus.valueOf(claim.getStatus().name());
                    } catch (Exception ex) {
                        claim.setStatus(Claim.ClaimStatus.CREATED);
                    }
                    claimRepository.save(claim);
                    updatedCount++;

                    log.info("Updated claim {} with fraud score: {}%",
                           claim.getClaimId(), fraudScore);
                }
            }

            if (updatedCount > 0) {
                log.info("SIU Data Initialization completed: {} claims updated with fraud scores",
                        updatedCount);
            } else {
                log.info("SIU Data Initialization: All claims already have fraud scores");
            }

        } catch (Exception e) {
            log.error("Error during SIU data initialization: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate realistic fraud scores based on claim characteristics.
     * Most claims should be low-risk, with some medium and few high-risk claims.
     */
    private double generateRealisticFraudScore(Claim claim) {
        // Base fraud score distribution (realistic insurance scenario)
        double baseScore;

        // 70% of claims are low-risk (0-40%)
        // 25% of claims are medium-risk (40-70%)
        // 5% of claims are high-risk (70-100%)
        double distribution = random.nextDouble();

        if (distribution < 0.70) {
            // Low risk: 0-40%
            baseScore = random.nextDouble() * 40.0;
        } else if (distribution < 0.95) {
            // Medium risk: 40-70%
            baseScore = 40.0 + (random.nextDouble() * 30.0);
        } else {
            // High risk: 70-100%
            baseScore = 70.0 + (random.nextDouble() * 30.0);
        }

        // Adjust based on claim amount (higher amounts slightly increase fraud risk)
        if (claim.getClaimAmount() != null && claim.getClaimAmount() > 200000.0) {
            baseScore += 5.0 + (random.nextDouble() * 10.0); // Add 5-15%
        } else if (claim.getClaimAmount() != null && claim.getClaimAmount() > 500000.0) {
            baseScore += 15.0 + (random.nextDouble() * 15.0); // Add 15-30%
        }

        // Ensure score stays within bounds
        return Math.min(100.0, Math.max(0.0,
                        Math.round(baseScore * 10.0) / 10.0)); // Round to 1 decimal place
    }
}