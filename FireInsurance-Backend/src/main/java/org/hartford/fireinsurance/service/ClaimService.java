
package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.CreateClaimRequest;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.Claim.ClaimStatus;
import org.hartford.fireinsurance.model.ClaimInspection;
import org.hartford.fireinsurance.model.RiskLevel;
import org.hartford.fireinsurance.service.BlacklistService;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Policy;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.SiuInvestigator;
import org.hartford.fireinsurance.model.Underwriter;
import org.hartford.fireinsurance.model.RiskLevel;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.hartford.fireinsurance.repository.NotificationPreferenceRepository;
import org.hartford.fireinsurance.repository.PolicySubscriptionRepository;
import org.hartford.fireinsurance.repository.SiuInvestigatorRepository;
import org.hartford.fireinsurance.repository.UnderwriterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ClaimService {
    // Helper to get any available underwriter (for SIU clear logic)
    public Underwriter getAnyAvailableUnderwriter() {
        return underwriterRepository.findAll().stream().findFirst().orElse(null);
    }

    // Helper to save claim (for SIU clear logic)
    public Claim saveClaim(Claim claim) {
        return claimRepository.save(claim);
    }

    private static final Logger log = LoggerFactory.getLogger(ClaimService.class);

    // Settlement calculation constants
    private static final double DEDUCTIBLE_PERCENTAGE = 0.05; // 5% deductible
    private static final double RESIDENTIAL_DEPRECIATION = 0.10; // 10%
    private static final double COMMERCIAL_DEPRECIATION = 0.15; // 15%
    private static final double INDUSTRIAL_DEPRECIATION = 0.20; // 20%
    private static final double DEFAULT_DEPRECIATION = 0.10; // Default 10%

    private final ClaimRepository claimRepository;
    private final PolicySubscriptionRepository subscriptionRepository;
    private final CustomerService customerService;
    private final org.hartford.fireinsurance.repository.ClaimInspectionRepository claimInspectionRepository;
    private final UnderwriterRepository underwriterRepository;
    private final SiuInvestigatorRepository siuInvestigatorRepository;

    private final EmailNotificationService emailNotificationService;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    // Blacklist integration
    private final BlacklistService blacklistService;

    // Optional: block blacklisted users from claim creation
    private static final boolean BLOCK_BLACKLISTED_USERS = false;

    public ClaimService(ClaimRepository claimRepository,
            PolicySubscriptionRepository subscriptionRepository,
            CustomerService customerService,
            org.hartford.fireinsurance.repository.ClaimInspectionRepository claimInspectionRepository,
            UnderwriterRepository underwriterRepository,
            SiuInvestigatorRepository siuInvestigatorRepository,
            EmailNotificationService emailNotificationService,
            NotificationPreferenceRepository notificationPreferenceRepository,
            BlacklistService blacklistService) {
        this.claimRepository = claimRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.customerService = customerService;
        this.claimInspectionRepository = claimInspectionRepository;
        this.underwriterRepository = underwriterRepository;
        this.siuInvestigatorRepository = siuInvestigatorRepository;
        this.emailNotificationService = emailNotificationService;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.blacklistService = blacklistService;
    }

    /**
     * Create a new claim (CUSTOMER only)
     */
    public Claim createClaim(String username, CreateClaimRequest request) {
        // Get the subscription
        PolicySubscription subscription = subscriptionRepository.findById(request.getSubscriptionId())
                .orElseThrow(
                        () -> new RuntimeException("Subscription not found with ID: " + request.getSubscriptionId()));

        // Security check: subscription must belong to logged-in customer
        if (!subscription.getCustomer().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized: Subscription does not belong to this customer");
        }

        // Extract identifiers for blacklist check
        String userUsername = subscription.getCustomer().getUser().getUsername();
        String userEmail = subscription.getCustomer().getUser().getEmail();
        String userPhone = subscription.getCustomer().getUser().getPhoneNumber();

        // Blacklist check
        boolean isBlacklisted = blacklistService.isBlacklisted(userUsername, userEmail, userPhone);
        if (isBlacklisted && BLOCK_BLACKLISTED_USERS) {
            throw new RuntimeException("Claim cannot be processed: user is blacklisted.");
        }

        // Create claim
        Claim claim = new Claim();
        claim.setSubscription(subscription);
        claim.setDescription(request.getDescription());
        claim.setClaimAmount(request.getClaimAmount());
        claim.setIncidentDate(request.getIncidentDate());
        claim.setCauseOfFire(request.getCauseOfFire());
        claim.setFirNumber(request.getFirNumber());
        claim.setFireBrigadeReportNumber(request.getFireBrigadeReportNumber());
        claim.setSalvageDetails(request.getSalvageDetails());
        claim.setLossType(request.getLossType());
        claim.setPoliceStation(request.getPoliceStation());
        claim.setContactPhoneNumber(request.getContactPhoneNumber());
        claim.setWitnessDetails(request.getWitnessDetails());
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setCreatedAt(LocalDateTime.now());

        // Default fraud score and risk
        claim.setFraudScore(0.0);
        claim.setRiskLevel(RiskLevel.LOW);

        // Blacklist impact on fraud detection
        if (isBlacklisted) {
            claim.setFraudScore(100.0);
            claim.setRiskLevel(RiskLevel.HIGH);
            String details = claim.getAnalysisDetails() != null ? claim.getAnalysisDetails() : "";
            details += (details.isEmpty() ? "" : "\n") + "User is blacklisted";
            claim.setAnalysisDetails(details);
            // SIU assignment logic (if present in your workflow)
            claim.setSiuStatus("PENDING_ASSIGNMENT");
        }

        return claimRepository.save(claim);
    }

    /**
     * Get all claims for a specific customer by username
     */
    public List<Claim> getClaimsByUsername(String username) {
        Customer customer = customerService.getCustomerByUsername(username);
        return claimRepository.findByCustomer(customer);
    }

    /**
     * Get all claims (ADMIN only)
     */
    public List<Claim> getAllClaims() {
        return claimRepository.findAll();
    }

    /**
     * Update claim status (ADMIN/SURVEYOR)
     * Sends email notifications to customer if preferences allow.
     */
    public Claim updateClaimStatus(Long id, ClaimStatus status) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + id));
        
        ClaimStatus oldStatus = claim.getStatus();
        claim.setStatus(status);
        Claim saved = claimRepository.save(claim);
        
        // Send email notification if status changed
        if (!oldStatus.equals(status)) {
            sendClaimStatusChangeEmail(saved);
        }
        
        return saved;
    }

    /**
     * Helper: Send email notification for claim status changes.
     * Non-blocking: errors are logged but don't fail the transaction.
     */
    private void sendClaimStatusChangeEmail(Claim claim) {
        try {
            Customer customer = claim.getSubscription().getCustomer();
            if (customer == null || customer.getUser() == null || customer.getUser().getEmail() == null) {
                log.warn("Cannot send claim status email: missing customer or email for claim {}", claim.getClaimId());
                return;
            }

            // Check user preferences
            notificationPreferenceRepository.findByUser(customer.getUser())
                    .filter(pref -> pref.getEmailEnabled() && pref.getEnabledEventKeys().contains("CLAIM_STATUS_CHANGE"))
                    .ifPresent(pref -> {
                        Map<String, String> vars = new HashMap<>();
                        vars.put("claimId", claim.getClaimId() != null ? claim.getClaimId().toString() : "Unknown");
                        vars.put("customerName", customer.getUser().getUsername());
                        vars.put("status", claim.getStatus() != null ? claim.getStatus().toString() : "Unknown");
                        vars.put("details", "Your claim has been updated. Please check your dashboard for more details.");
                        
                        emailNotificationService.sendEmailNotification(
                                customer.getUser().getEmail(),
                                "CLAIM_STATUS_CHANGE",
                                vars
                        );
                    });
        } catch (Exception e) {
            // Non-blocking: log but don't throw
            log.error("Error sending claim status change email for claim {}: {}", claim.getClaimId(), e.getMessage());
        }
    }

    /**
     * Get claim by ID
     */
    public Claim getClaimById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + id));
    }

    /**
     * Calculate settlement amount based on claim inspection and policy terms
     * Formula: MIN((EstimatedLoss - Deductible - Depreciation), MaxCoverage)
     */
    public Double calculateSettlementAmount(Claim claim) {
        log.info("Starting settlement calculation for Claim ID: {}", claim.getClaimId());

        Double estimatedLoss = getEstimatedLoss(claim);
        if (estimatedLoss == null || estimatedLoss <= 0) {
            log.warn("No valid estimated loss for Claim ID: {}. Settlement calculation skipped.", claim.getClaimId());
            claim.setEstimatedLoss(0.0);
            return null;
        }

        // Save estimated loss on the entity
        claim.setEstimatedLoss(estimatedLoss);
        log.info("Estimated Loss: ₹{}", estimatedLoss);

        // Get deductible from Policy
        double deductible = getCalculatedDeductible(claim);
        log.info("Deductible from Policy: ₹{}", deductible);

        // Calculate depreciation based on property age
        double depreciationRate = getDepreciationRate(claim);
        double depreciationAmount = estimatedLoss * depreciationRate;
        log.info("Depreciation Rate: {}%, Depreciation Amount: ₹{}", (depreciationRate * 100), depreciationAmount);

        // Calculate intermediate settlement (Loss - Deductible - Depreciation)
        double intermediateSettlement = estimatedLoss - deductible - depreciationAmount;
        log.info("Intermediate Settlement (Loss - Deductible - Depreciation): ₹{}", intermediateSettlement);

        // Ensure settlement is not negative
        if (intermediateSettlement < 0) {
            intermediateSettlement = 0;
            log.warn("Intermediate settlement was negative, set to ₹0");
        }

        // Apply Average Clause for under-insurance:
        // settlement = (sumInsured / propertyValue) * loss
        double averageClauseAdjusted = applyAverageClause(claim, intermediateSettlement);
        log.info("Average Clause Adjusted Settlement: ₹{}", averageClauseAdjusted);

        // Get maximum coverage from policy
        Double maxCoverage = getMaxCoverage(claim);
        if (maxCoverage == null) {
            log.warn("No max coverage found for Claim ID: {}. Using intermediate settlement.", claim.getClaimId());
            return averageClauseAdjusted;
        }
        log.info("Policy Max Coverage: ₹{}", maxCoverage);

        // Apply max coverage cap
        double finalSettlement = Math.min(averageClauseAdjusted, maxCoverage);
        log.info("Final Settlement Amount: ₹{} (capped at max coverage)", finalSettlement);

        return finalSettlement;
    }

    /**
     * Get estimated loss from claim inspection
     */
    public Double getEstimatedLoss(Claim claim) {
        if (claim.getEstimatedLoss() != null && claim.getEstimatedLoss() > 0) {
            return claim.getEstimatedLoss();
        }
        ClaimInspection inspection = claim.getClaimInspection();
        if (inspection != null && inspection.getEstimatedLoss() != null) {
            return inspection.getEstimatedLoss();
        }

        log.info("No inspection found, using claimed amount as estimated loss");
        return claim.getClaimAmount() != null ? claim.getClaimAmount() : 0.0;
    }

    public Double getCalculatedDeductible(Claim claim) {
        try {
            Double d = claim.getSubscription().getPolicy().getDeductible();
            return d != null ? d : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public Double getCalculatedDepreciation(Claim claim) {
        Double estLoss = getEstimatedLoss(claim);
        if (estLoss == null)
            return 0.0;
        return estLoss * getDepreciationRate(claim);
    }

    /**
     * Get depreciation rate based on property type
     */
    public double getDepreciationRate(Claim claim) {
        try {
            PolicySubscription subscription = claim.getSubscription();
            if (subscription != null && subscription.getProperty() != null) {
                Property property = subscription.getProperty();
                Integer age = property.getPropertyAge();
                if (age == null)
                    age = 0;

                log.info("Property Age: {} years", age);
                if (age <= 5)
                    return 0.05;
                if (age <= 10)
                    return 0.10;
                if (age <= 20)
                    return 0.20;
                return 0.30;
            }
        } catch (Exception e) {
            log.error("Error getting property age for depreciation: {}", e.getMessage());
        }

        log.info("Using default depreciation rate: 10%");
        return 0.10; // Fallback default
    }

    /**
     * Get maximum coverage from policy
     */
    private Double getMaxCoverage(Claim claim) {
        try {
            PolicySubscription subscription = claim.getSubscription();
            if (subscription != null && subscription.getPolicy() != null) {
                Policy policy = subscription.getPolicy();
                return policy.getMaxCoverageAmount();
            }
        } catch (Exception e) {
            log.error("Error getting max coverage: {}", e.getMessage());
        }
        return null;
    }

    private double applyAverageClause(Claim claim, double candidateSettlement) {
        try {
            PolicySubscription subscription = claim.getSubscription();
            if (subscription == null) {
                return candidateSettlement;
            }

            Double sumInsured = subscription.getRequestedCoverage();
            if (sumInsured == null || sumInsured <= 0) {
                sumInsured = subscription.getPolicy() != null ? subscription.getPolicy().getMaxCoverageAmount() : null;
            }

            Double propertyValue = subscription.getPropertyValue();
            if (propertyValue == null || propertyValue <= 0) {
                return candidateSettlement;
            }

            if (sumInsured == null || sumInsured <= 0 || sumInsured >= propertyValue) {
                return candidateSettlement;
            }

            double ratio = sumInsured / propertyValue;
            return Math.max(0.0, candidateSettlement * ratio);
        } catch (Exception e) {
            log.warn("Failed to apply average clause; falling back. Reason: {}", e.getMessage());
            return candidateSettlement;
        }
    }

    // ========== OLD METHODS (for backward compatibility) ==========

    public Claim raiseClaim(Claim claim) {
        claim.setStatus(ClaimStatus.SUBMITTED);
        return claimRepository.save(claim);
    }

    public Claim approveClaim(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        log.info("=== APPROVING CLAIM ID: {} ===", claimId);

        // Calculate settlement amount before approval
        Double settlementAmount = calculateSettlementAmount(claim);
        if (settlementAmount != null) {
            claim.setSettlementAmount(settlementAmount);
            log.info("Claim Settlement Calculated: ₹{}", settlementAmount);
        } else {
            log.warn("Settlement amount could not be calculated for Claim ID: {}", claimId);
        }

        claim.setStatus(ClaimStatus.APPROVED);

        // Update associated inspection status
        updateRelatedInspectionStatus(claim,
                org.hartford.fireinsurance.model.ClaimInspection.ClaimInspectionStatus.APPROVED);

        // RESET NCB when claim is approved
        PolicySubscription subscription = claim.getSubscription();
        if (subscription != null) {
            subscription.setClaimFreeYears(0);
            subscription.setNcbDiscount(0.0);
            subscription.setLastClaimDate(java.time.LocalDateTime.now());
            subscriptionRepository.save(subscription);
            log.info("NCB reset for Subscription ID: {} due to approved claim", subscription.getSubscriptionId());
        }

        Claim savedClaim = claimRepository.save(claim);
        log.info("=== CLAIM APPROVED - Settlement: ₹{} ===", savedClaim.getSettlementAmount());

        return savedClaim;
    }

    public Claim rejectClaim(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        log.info("Rejecting Claim ID: {}", claimId);
        claim.setStatus(ClaimStatus.REJECTED);

        // Update associated inspection status
        updateRelatedInspectionStatus(claim,
                org.hartford.fireinsurance.model.ClaimInspection.ClaimInspectionStatus.REJECTED);

        return claimRepository.save(claim);
    }

    public Claim assignUnderwriter(Long claimId, Long underwriterId) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));

        // Enforce: If risk is HIGH, it MUST be SIU_CLEARED first.
        // For LOW/MEDIUM risk, we allow assignment directly from SUBMITTED or regular review statuses.
        if (claim.getRiskLevel() == RiskLevel.HIGH && claim.getStatus() != ClaimStatus.SIU_CLEARED) {
            throw new IllegalStateException("HIGH risk claims must be cleared by SIU (Status: SIU_CLEARED) before underwriter assignment.");
        }

        Underwriter underwriter = underwriterRepository.findById(underwriterId)
            .orElseThrow(() -> new RuntimeException("Underwriter not found with ID: " + underwriterId));

        claim.setUnderwriter(underwriter);
        
        // Transition status to UNDER_REVIEW once assigned
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        
        return claimRepository.save(claim);
    }

    public Claim assignSiuInvestigator(Long claimId, Long investigatorId) {
        Claim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));

        // Enforce: Only HIGH risk to SIU
        if (claim.getRiskLevel() != RiskLevel.HIGH) {
            throw new IllegalStateException("Only HIGH risk claims can be assigned to SIU.");
        }

        SiuInvestigator investigator = siuInvestigatorRepository.findById(investigatorId)
            .orElseThrow(() -> new RuntimeException("SIU investigator not found with ID: " + investigatorId));

        claim.setSiuInvestigator(investigator);
        claim.setSiuStatus("UNDER_INVESTIGATION");
        return claimRepository.save(claim);
    }

    private void updateRelatedInspectionStatus(Claim claim,
            org.hartford.fireinsurance.model.ClaimInspection.ClaimInspectionStatus status) {
        try {
            java.util.List<org.hartford.fireinsurance.model.ClaimInspection> inspections = claimInspectionRepository
                    .findAll();
            for (org.hartford.fireinsurance.model.ClaimInspection inspection : inspections) {
                if (inspection.getClaim() != null && inspection.getClaim().getClaimId().equals(claim.getClaimId())) {
                    inspection.setStatus(status);
                    claimInspectionRepository.save(inspection);
                }
            }
        } catch (Exception e) {
            log.error("Error updating inspection status: {}", e.getMessage());
        }
    }
}
