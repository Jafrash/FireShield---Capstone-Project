package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.PremiumBreakdownLineItem;
import org.hartford.fireinsurance.dto.PremiumBreakdownResponse;
import org.hartford.fireinsurance.dto.SubscribeRequest;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Inspection;
import org.hartford.fireinsurance.model.Policy;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.PolicySubscription.SubscriptionStatus;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.Underwriter;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.NotificationPreferenceRepository;
import org.hartford.fireinsurance.repository.PolicySubscriptionRepository;
import org.hartford.fireinsurance.repository.UnderwriterRepository;
import org.hartford.fireinsurance.exception.InvalidRequestException;
import org.hartford.fireinsurance.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PolicySubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(PolicySubscriptionService.class);

    private final PolicySubscriptionRepository subscriptionRepository;
    private final CustomerService customerService;
    private final PropertyService propertyService;
    private final PolicyService policyService;
    private final InspectionService inspectionService;
    private final SurveyorService surveyorService;
    private final UnderwriterRepository underwriterRepository;
    private final PremiumCalculationService premiumCalculationService;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final EmailNotificationService emailNotificationService;

    public PolicySubscriptionService(
            PolicySubscriptionRepository subscriptionRepository,
            CustomerService customerService,
            PropertyService propertyService,
            PolicyService policyService,
            InspectionService inspectionService,
            SurveyorService surveyorService,
            UnderwriterRepository underwriterRepository,
            PremiumCalculationService premiumCalculationService,
            NotificationPreferenceRepository notificationPreferenceRepository,
            EmailNotificationService emailNotificationService) {
        this.subscriptionRepository = subscriptionRepository;
        this.customerService = customerService;
        this.propertyService = propertyService;
        this.policyService = policyService;
        this.inspectionService = inspectionService;
        this.surveyorService = surveyorService;
        this.underwriterRepository = underwriterRepository;
        this.premiumCalculationService = premiumCalculationService;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Production-style premium formula:
     * Premium = ApprovedCoverage × BaseRate × RiskFactor
     */
    public double calculatePremium(PolicySubscription subscription, Double riskScore, Inspection inspection) {
        return premiumCalculationService.calculatePremium(subscription, riskScore, inspection);
    }

    /**
     * Get risk multiplier for a given risk score
     */
    public double getRiskMultiplier(double riskScore) {
        return premiumCalculationService.getRiskMultiplier(riskScore);
    }

    public PremiumBreakdownResponse getPremiumBreakdown(Long subscriptionId) {
        PolicySubscription subscription = getSubscriptionById(subscriptionId);
        Policy policy = subscription.getPolicy();

        if (policy == null) {
            throw new RuntimeException("Policy not found for subscription: " + subscriptionId);
        }

        Double totalPremium = subscription.getPremiumAmount();
        if (totalPremium == null || totalPremium <= 0) {
            throw new RuntimeException("Premium has not been calculated for this subscription yet.");
        }

        syncCalculatedPremium(subscription);

        Double approvedCoverage = round2(getApprovedCoverage(subscription));
        Double requestedCoverage = subscription.getRequestedCoverage() != null ? round2(subscription.getRequestedCoverage()) : approvedCoverage;
        Double maxCoverage = policy.getMaxCoverageAmount() != null ? round2(policy.getMaxCoverageAmount()) : approvedCoverage;
        double effectiveMaxCoverage = maxCoverage != null && maxCoverage > 0 ? maxCoverage : approvedCoverage;
        double basePremiumReference = policy.getBasePremium() != null ? round2(policy.getBasePremium()) : 0.0;
        double preciseBaseRate = effectiveMaxCoverage > 0 ? (basePremiumReference / effectiveMaxCoverage) : 0.003;
        double basePremiumForCoverage = round2(approvedCoverage * preciseBaseRate);

        Double riskScore = subscription.getRiskScore() != null ? subscription.getRiskScore() : 5.0;
        double baseRiskMultiplier = round2(getRiskMultiplier(riskScore));

        // Retrieve inspection from subscription if not provided
        Inspection inspection = subscription.getPropertyInspection();

        // 2. Inspection-Verified COPE Factors (High Fidelity)
        double constructionLoadingFactor = 0.0;
        double roofLoadingFactor = 0.0;
        double detailedOccupancyLoadingFactor = 0.0;
        double electricalAuditAdjustmentFactor = 0.0;
        double hazardousMaterialsLoadingFactor = 0.0;
        double proximityAdjustmentFactor = 0.0;

        if (inspection != null) {
            // Construction Type
            if (inspection.getConstructionType() != null) {
                switch (inspection.getConstructionType().toUpperCase()) {
                    case "FRAME" -> constructionLoadingFactor = 0.20;
                    case "JOISTED_MASONRY" -> constructionLoadingFactor = 0.10;
                    case "NON_COMBUSTIBLE" -> constructionLoadingFactor = -0.05;
                    case "FIRE_RESISTIVE" -> constructionLoadingFactor = -0.15;
                }
            }

            // Roofing
            if (inspection.getRoofType() != null) {
                switch (inspection.getRoofType().toUpperCase()) {
                    case "WOOD" -> roofLoadingFactor = 0.15;
                    case "ASPHALT" -> roofLoadingFactor = 0.05;
                    case "CONCRETE" -> roofLoadingFactor = -0.05;
                }
            }

            // Occupancy
            if (inspection.getOccupancyType() != null) {
                switch (inspection.getOccupancyType().toUpperCase()) {
                    case "INDUSTRIAL" -> detailedOccupancyLoadingFactor = 0.25;
                    case "WAREHOUSE" -> detailedOccupancyLoadingFactor = 0.15;
                    case "COMMERCIAL" -> detailedOccupancyLoadingFactor = 0.10;
                    case "RESIDENTIAL" -> detailedOccupancyLoadingFactor = 0.0;
                }
            } else if ("INDUSTRIAL".equalsIgnoreCase(subscription.getOccupancyType())) {
                detailedOccupancyLoadingFactor = 0.15;
            }

            // Electrical & Hazards
            if ("FAIL".equalsIgnoreCase(inspection.getElectricalAuditStatus())) {
                electricalAuditAdjustmentFactor = 0.30;
            } else if ("PASS".equalsIgnoreCase(inspection.getElectricalAuditStatus())) {
                electricalAuditAdjustmentFactor = -0.05;
            }

            if (Boolean.TRUE.equals(inspection.getHazardousMaterialsPresent())) {
                hazardousMaterialsLoadingFactor = 0.20;
            }

            // Exposure
            if (inspection.getAdjacentBuildingDistance() != null) {
                if (inspection.getAdjacentBuildingDistance() < 5) proximityAdjustmentFactor = 0.15;
                else if (inspection.getAdjacentBuildingDistance() > 25) proximityAdjustmentFactor = -0.05;
            }
        }

        double hazardousGoodsLoadingFactor = hasText(subscription.getHazardousGoods()) ? 0.15 : 0.0;
        double declinedInsuranceLoadingFactor = Boolean.TRUE.equals(subscription.getInsuranceDeclinedBefore()) ? 0.15 : 0.0;
        double fireSafetyDiscountFactor = (inspection != null && Boolean.TRUE.equals(inspection.getFireSafetyAvailable())) ? -0.05 : 0.0;
        double sprinklerDiscountFactor = (inspection != null && Boolean.TRUE.equals(inspection.getSprinklerSystem())) ? -0.10 : 0.0;

        // Calculate rawRiskFactor using premiumCalculationService
        double rawRiskFactor = premiumCalculationService.computeRiskFactor(subscription, riskScore, inspection);
        double finalRiskFactor = round2(rawRiskFactor);

        List<PremiumBreakdownLineItem> lineItems = new ArrayList<>();
        lineItems.add(new PremiumBreakdownLineItem(
                "Base premium for approved coverage",
                "Base premium adjusted from the policy reference premium using approved coverage.",
                round2(basePremiumForCoverage),
                "BASE"
        ));

        // Start adding adjustments
        addLineItemIfNonZero(lineItems, "Risk score multiplier", "Property risk score of " + round2(riskScore) + "/10 sets the starting multiplier.", basePremiumForCoverage * (baseRiskMultiplier - 1.0));
        
        // COPE Breakdown
        if (inspection != null) {
            String cType = inspection.getConstructionType() != null ? inspection.getConstructionType() : "N/A";
            addLineItemIfNonZero(lineItems, "Construction Type (" + cType + ")", "Adjustment based on structural fire resistance classes.", basePremiumForCoverage * constructionLoadingFactor);
            
            String rType = inspection.getRoofType() != null ? inspection.getRoofType() : "N/A";
            addLineItemIfNonZero(lineItems, "Roofing Material (" + rType + ")", "Adjustment based on roof material combustibility.", basePremiumForCoverage * roofLoadingFactor);
            
            addLineItemIfNonZero(lineItems, "Occupancy Tier Loading", "Loading based on verified space usage and hazard levels.", basePremiumForCoverage * detailedOccupancyLoadingFactor);
            
            String eStatus = inspection.getElectricalAuditStatus() != null ? inspection.getElectricalAuditStatus() : "N/A";
            addLineItemIfNonZero(lineItems, "Electrical Safety (" + eStatus + ")", "Adjustment based on internal electrical installation audit.", basePremiumForCoverage * electricalAuditAdjustmentFactor);
            
            addLineItemIfNonZero(lineItems, "Hazardous Materials Loading", "Loading for presence of verified high-risk combustible materials.", basePremiumForCoverage * hazardousMaterialsLoadingFactor);
            
            addLineItemIfNonZero(lineItems, "Environmental Exposure", "Adjustment based on proximity to adjacent buildings (Exposure).", basePremiumForCoverage * proximityAdjustmentFactor);
        }

        addLineItemIfNonZero(lineItems, "Proposals: Hazardous Goods", "Additional loading because hazardous goods were declared in proposal.", basePremiumForCoverage * hazardousGoodsLoadingFactor);
        addLineItemIfNonZero(lineItems, "Proposals: Insurance History", "Additional loading because prior insurance was declined.", basePremiumForCoverage * declinedInsuranceLoadingFactor);
        
        addLineItemIfNonZero(lineItems, "Fire Safety Credit", "Discount applied because basic fire safety measures are verified.", basePremiumForCoverage * fireSafetyDiscountFactor);
        addLineItemIfNonZero(lineItems, "Automatic Sprinkler Credit", "Discount applied because a sprinkler system is verified.", basePremiumForCoverage * sprinklerDiscountFactor);

        int installmentMonths = policy.getDurationMonths() != null && policy.getDurationMonths() > 0 ? policy.getDurationMonths() : 12;
        double monthlyPremium = round2(totalPremium / installmentMonths);

        PremiumBreakdownResponse response = new PremiumBreakdownResponse();
        response.setSubscriptionId(subscription.getSubscriptionId());
        response.setPolicyName(policy.getPolicyName());
        response.setTotalPremium(round2(totalPremium));
        response.setMonthlyPremium(monthlyPremium);
        response.setInstallmentMonths(installmentMonths);
        response.setApprovedCoverage(approvedCoverage);
        response.setRequestedCoverage(requestedCoverage);
        response.setMaxCoverageAmount(maxCoverage);
        response.setBasePremiumReference(round2(basePremiumReference));
        response.setDerivedBaseRate(round4(preciseBaseRate));
        response.setBasePremiumForCoverage(round2(basePremiumForCoverage));
        response.setRiskScore(round2(riskScore));
        response.setBaseRiskMultiplier(baseRiskMultiplier);
        response.setRawRiskFactor(rawRiskFactor);
        response.setFinalRiskFactor(finalRiskFactor);
        response.setDeductible(policy.getDeductible() != null ? round2(policy.getDeductible()) : 0.0);
        response.setLineItems(lineItems);
        return response;
    }

    private double computeRiskFactor(PolicySubscription subscription, Double riskScore, Inspection inspection) {
        return premiumCalculationService.computeRiskFactor(subscription, riskScore, inspection);
    }

    private double getApprovedCoverage(PolicySubscription subscription) {
        return premiumCalculationService.getApprovedCoverage(subscription);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private void addLineItemIfNonZero(List<PremiumBreakdownLineItem> lineItems, String label, String description, double amount) {
        double roundedAmount = round2(amount);
        if (roundedAmount == 0.0) {
            return;
        }
        lineItems.add(new PremiumBreakdownLineItem(label, description, roundedAmount, roundedAmount >= 0 ? "ADJUSTMENT" : "DISCOUNT"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Get risk category label
     */
    public String getRiskCategory(double riskScore) {
        if (riskScore <= 2) return "Low Risk";
        else if (riskScore <= 4) return "Normal Risk";
        else if (riskScore <= 6) return "Moderate Risk";
        else if (riskScore <= 8) return "High Risk";
        else return "Very High Risk";
    }

    /**
     * Customer submits policy proposal.
     */
    public PolicySubscription subscribe(String username, SubscribeRequest request) {
        try {
            logger.info("[SUB-START] Processing subscription for user: {}", username);
            
            if (request.getPropertyId() == null) throw new InvalidRequestException("propertyId", "Property ID is mandatory");
            if (request.getPolicyId() == null) throw new InvalidRequestException("policyId", "Policy ID is mandatory");
            
            logger.debug("[SUB-FETCH] Fetching customer, property, and policy");
            Customer customer = customerService.getCustomerByUsername(username);
            Property property = propertyService.getPropertyById(request.getPropertyId());
            Policy policy = policyService.getPolicyById(request.getPolicyId());

            logger.debug("[SUB-VALIDATE] Validating ownership and duplicates");
            // Security check: property must belong to customer
            if (property.getCustomer() == null || property.getCustomer().getUser() == null || 
                !property.getCustomer().getUser().getUsername().equals(username)) {
                throw new RuntimeException("Unauthorized: Property ownership mismatch for " + username);
            }

            // Prevent duplicate subscriptions: Handle existing apps gracefully
            List<PolicySubscription> existingSubs = subscriptionRepository.findByProperty(property);
            if (existingSubs != null) {
                for (PolicySubscription sub : existingSubs) {
                    if (sub.getPolicy() != null && sub.getPolicy().getPolicyId().equals(policy.getPolicyId())) {
                        SubscriptionStatus s = sub.getStatus();
                        if (s == SubscriptionStatus.ACTIVE || s == SubscriptionStatus.APPROVED || s == SubscriptionStatus.PAYMENT_PENDING) {
                            logger.warn("[SUB-DUPE] Blocking: Active policy exists for property {} (Sub ID: {})", property.getPropertyId(), sub.getSubscriptionId());
                            throw new org.hartford.fireinsurance.exception.DuplicateResourceException("An active or approved policy already exists for this property.");
                        } else if (s == SubscriptionStatus.SUBMITTED || s == SubscriptionStatus.PENDING || s == SubscriptionStatus.REQUESTED || s == SubscriptionStatus.UNDER_REVIEW) {
                            // If it's just a draft/submitted/pending from a previous session, we'll allow a re-submission by removing the "zombie" entry.
                            logger.info("[SUB-CLEANUP] Removing previous pending application (ID: {}) to allow re-submission", sub.getSubscriptionId());
                            subscriptionRepository.delete(sub);
                        }
                    }
                }
            }

            logger.debug("[SUB-CREATE] Mapping fields to entity");
            PolicySubscription subscription = new PolicySubscription();
            subscription.setCustomer(customer);
            subscription.setProperty(property);
            subscription.setPolicy(policy);
            subscription.setStatus(SubscriptionStatus.SUBMITTED);
            
            subscription.setStartDate(LocalDate.now());
            int duration = (policy.getDurationMonths() != null) ? policy.getDurationMonths() : 12;
            subscription.setEndDate(LocalDate.now().plusMonths(duration));
            
            subscription.setBasePremiumAmount(policy.getBasePremium() != null ? policy.getBasePremium() : 0.0);
            subscription.setRequestedCoverage(request.getRequestedCoverage());

            // Underwriting fields
            subscription.setConstructionType(request.getConstructionType());
            subscription.setRoofType(request.getRoofType());
            subscription.setNumberOfFloors(request.getNumberOfFloors() != null ? request.getNumberOfFloors() : 1);
            subscription.setOccupancyType(request.getOccupancyType());
            subscription.setManufacturingProcess(request.getManufacturingProcess());
            subscription.setHazardousGoods(request.getHazardousGoods());
            subscription.setPreviousLossHistory(request.getPreviousLossHistory());
            subscription.setInsuranceDeclinedBefore(request.getInsuranceDeclinedBefore() != null ? request.getInsuranceDeclinedBefore() : false);
            subscription.setPropertyValue(request.getPropertyValue() != null ? request.getPropertyValue() : 0.0);
            subscription.setDeclarationAccepted(request.getDeclarationAccepted() != null ? request.getDeclarationAccepted() : true);
            
            subscription.setPremiumAmount(subscription.getBasePremiumAmount());
            subscription.setPaymentReceived(false);
            subscription.setRenewalEligible(false);
            subscription.setRenewalCount(0);

            System.err.println("[SUB-SAVE] Calling subscriptionRepository.save(subscription)");
            PolicySubscription savedSubscription = subscriptionRepository.save(subscription);
            System.err.println("[SUB-SUCCESS] Subscription created with ID: " + savedSubscription.getSubscriptionId());
            logger.info("[SUB-SUCCESS] Subscription created with ID: {}", savedSubscription.getSubscriptionId());
            logger.info("Successfully created subscription ID: {} for user: {}", savedSubscription.getSubscriptionId(), username);

            // Optional: send email
            try {
                Map<String, String> vars = new HashMap<>();
                vars.put("customerName", customer.getUser().getUsername());
                vars.put("policyNumber", "SUB" + savedSubscription.getSubscriptionId());
                vars.put("policyName", policy.getPolicyName() != null ? policy.getPolicyName() : "Policy");
                sendPolicyEmailIfEnabled(savedSubscription, "POLICY_SUBMITTED", vars);
            } catch (Exception emailEx) {
                logger.error("Failed to process email for subscription {}: {}", savedSubscription.getSubscriptionId(), emailEx.getMessage());
                // Don't fail the whole transaction for email issues
            }

            return savedSubscription;
        } catch (Exception ex) {
            System.err.println("[SUB-ERROR] Exception in subscribe for user " + username + ": " + ex.getMessage());
            ex.printStackTrace();
            logger.error("Error in subscribe operation for user {}: {}", username, ex.getMessage(), ex);
            throw ex; // Re-throw to be handled by GlobalExceptionHandler
        }
    }

    /**
     * Admin assigns surveyor to a requested subscription for property inspection
     */
    public PolicySubscription assignSurveyorForInspection(Long subscriptionId, Long surveyorId) {
        PolicySubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + subscriptionId));

        if (subscription.getStatus() != SubscriptionStatus.SUBMITTED &&
            subscription.getStatus() != SubscriptionStatus.UNDER_REVIEW &&
            subscription.getStatus() != SubscriptionStatus.REQUESTED) {
            throw new RuntimeException("Can only assign surveyor to SUBMITTED/UNDER_REVIEW subscriptions. Current status: " + subscription.getStatus());
        }

        // Create inspection and assign surveyor
        Inspection inspection = inspectionService.assignSurveyor(
                subscription.getProperty().getPropertyId(),
                surveyorId
        );

        // Link inspection to subscription
        subscription.setPropertyInspection(inspection);
        subscription.setStatus(SubscriptionStatus.INSPECTION_PENDING);

        PolicySubscription savedSubscription = subscriptionRepository.save(subscription);

        Map<String, String> vars = new HashMap<>();
        vars.put("customerName", safeCustomer(savedSubscription));
        vars.put("policyNumber", "SUB" + savedSubscription.getSubscriptionId());
        vars.put("effectiveDate", savedSubscription.getStartDate() != null ? savedSubscription.getStartDate().toString() : "After payment");
        vars.put("premium", savedSubscription.getPremiumAmount() != null ? savedSubscription.getPremiumAmount().toString() : "N/A");
        sendPolicyEmailIfEnabled(savedSubscription, "POLICY_APPROVAL", vars);

        return savedSubscription;
    }

    /**
     * Called by InspectionService when surveyor completes inspection
     * This calculates the premium based on risk score
     */
    public PolicySubscription markAsInspected(Long subscriptionId) {
        PolicySubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + subscriptionId));

        // Get risk score from property (updated by inspection)
        Double propertyRiskScore = subscription.getProperty().getRiskScore();
        
        if (propertyRiskScore == null) {
            throw new RuntimeException("Cannot calculate premium: Property risk score not available");
        }

        subscription.setRiskScore(propertyRiskScore);
        syncCalculatedPremium(subscription);
        subscription.setStatus(SubscriptionStatus.INSPECTED);

        return subscriptionRepository.save(subscription);
    }

    /**
     * Admin approves an inspected subscription
     * Premium must already be calculated (after inspection)
     */
    public PolicySubscription approveSubscription(Long id) {
        PolicySubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + id));

        // Can only approve subscriptions that have been inspected
        if (subscription.getStatus() != SubscriptionStatus.INSPECTED &&
            subscription.getStatus() != SubscriptionStatus.INSPECTION_PENDING) {
            throw new RuntimeException("Only INSPECTED/INSPECTION_PENDING subscriptions can be approved. Current status: " + subscription.getStatus());
        }

        // Verify property inspection exists and is completed
        if (subscription.getPropertyInspection() == null) {
            throw new RuntimeException("Cannot approve subscription without property inspection");
        }

        // Verify premium has been calculated
        if (subscription.getPremiumAmount() == null) {
            throw new RuntimeException("Cannot approve subscription: Premium not calculated. Please ensure inspection is complete.");
        }

        // Legal workflow: wait for premium payment before activation.
        subscription.setStatus(SubscriptionStatus.PAYMENT_PENDING);
        subscription.setPaymentReceived(false);

        return subscriptionRepository.save(subscription);
    }

    /**
     * Simple payment acceptance flow requested by frontend.
     */
    public PolicySubscription acceptPayment(Long id) {
        PolicySubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + id));

        if (subscription.getStatus() != SubscriptionStatus.PAYMENT_PENDING &&
            subscription.getStatus() != SubscriptionStatus.APPROVED) {
            throw new RuntimeException("Payment can only be accepted for PAYMENT_PENDING/APPROVED subscriptions. Current status: " + subscription.getStatus());
        }

        subscription.setPaymentReceived(true);
        subscription.setPaymentDate(LocalDateTime.now());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());

        Integer durationMonths = subscription.getPolicy().getDurationMonths() != null
                ? subscription.getPolicy().getDurationMonths() : 12;
        subscription.setEndDate(LocalDate.now().plusMonths(durationMonths));

        generateLegalDocuments(subscription);
        return subscriptionRepository.save(subscription);
    }

    private void generateLegalDocuments(PolicySubscription subscription) {
        try {
            Path outDir = Paths.get("uploads", "legal-docs");
            Files.createDirectories(outDir);

            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String ref = "SUB" + subscription.getSubscriptionId();
            String coverName = "CoverNote-" + ref + "-" + stamp + ".pdf";
            String policyName = "PolicyDocument-" + ref + "-" + stamp + ".pdf";

            String details = "Customer: " + safeCustomer(subscription) + "\n"
                    + "Policy Number: " + ref + "\n"
                    + "Property Address: " + (subscription.getProperty() != null ? subscription.getProperty().getAddress() : "N/A") + "\n"
                    + "Sum Insured: " + getApprovedCoverage(subscription) + "\n"
                    + "Premium: " + subscription.getPremiumAmount() + "\n"
                    + "Policy Period: " + subscription.getStartDate() + " to " + subscription.getEndDate() + "\n";

            Files.writeString(outDir.resolve(coverName), "COVER NOTE\n\n" + details,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(outDir.resolve(policyName), "POLICY DOCUMENT\n\n" + details,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            subscription.setCoverNoteFileName(coverName);
            subscription.setPolicyDocumentFileName(policyName);
        } catch (IOException ignored) {
            // Best-effort generation to keep payment flow uninterrupted.
        }
    }

    public byte[] getCoverNoteBytes(Long subscriptionId) {
        PolicySubscription subscription = getSubscriptionById(subscriptionId);
        return getLegalDocumentBytes(subscription, true);
    }

    public byte[] getPolicyDocumentBytes(Long subscriptionId) {
        PolicySubscription subscription = getSubscriptionById(subscriptionId);
        return getLegalDocumentBytes(subscription, false);
    }

    private byte[] getLegalDocumentBytes(PolicySubscription subscription, boolean coverNote) {
        String fileName = coverNote ? subscription.getCoverNoteFileName() : subscription.getPolicyDocumentFileName();

        if (fileName != null && !fileName.isBlank()) {
            try {
                Path filePath = Paths.get("uploads", "legal-docs", fileName);
                if (Files.exists(filePath)) {
                    return Files.readAllBytes(filePath);
                }
            } catch (IOException ignored) {
                // Fall back to generated text content below.
            }
        }

        String title = coverNote ? "COVER NOTE" : "POLICY DOCUMENT";
        String content = title + "\n\n" + buildLegalDocumentDetails(subscription);
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private String buildLegalDocumentDetails(PolicySubscription subscription) {
        String start = subscription.getStartDate() != null ? subscription.getStartDate().toString() : "N/A";
        String end = subscription.getEndDate() != null ? subscription.getEndDate().toString() : "N/A";
        return "Customer: " + safeCustomer(subscription) + "\n"
                + "Policy Number: SUB" + subscription.getSubscriptionId() + "\n"
                + "Property Address: " + (subscription.getProperty() != null ? subscription.getProperty().getAddress() : "N/A") + "\n"
                + "Sum Insured: " + getApprovedCoverage(subscription) + "\n"
                + "Premium: " + (subscription.getPremiumAmount() != null ? subscription.getPremiumAmount() : 0.0) + "\n"
                + "Policy Period: " + start + " to " + end + "\n";
    }

    private String safeCustomer(PolicySubscription subscription) {
        try {
            return subscription.getCustomer().getUser().getUsername();
        } catch (Exception ignored) {
            return "Customer";
        }
    }

    /**
     * Admin rejects a subscription request
     */
    public PolicySubscription rejectSubscription(Long id) {
        PolicySubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + id));

        // Can reject at any stage before ACTIVE
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("Cannot reject an ACTIVE subscription. Use cancel instead.");
        }

        subscription.setStatus(SubscriptionStatus.REJECTED);
        PolicySubscription savedSubscription = subscriptionRepository.save(subscription);

        Map<String, String> vars = new HashMap<>();
        vars.put("customerName", safeCustomer(savedSubscription));
        vars.put("reason", "Please contact support for details on this decision.");
        sendPolicyEmailIfEnabled(savedSubscription, "POLICY_REJECTION", vars);

        return savedSubscription;
    }

    /**
     * Get all subscriptions for a customer by username
     */
    public List<PolicySubscription> getSubscriptionsByUsername(String username) {
        Customer customer = customerService.getCustomerByUsername(username);
        return subscriptionRepository.findByCustomer(customer).stream()
                .map(this::syncCalculatedPremium)
                .toList();
    }

    /**
     * Get all subscriptions (Admin only)
     */
    public List<PolicySubscription> getAllSubscriptions() {
        return subscriptionRepository.findAll().stream()
                .map(this::syncCalculatedPremium)
                .toList();
    }

    /**
     * Cancel a subscription (Admin only)
     */
    public PolicySubscription cancelSubscription(Long id) {
        PolicySubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + id));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        return subscriptionRepository.save(subscription);
    }

    /**
     * Get subscription by ID
     */
    public PolicySubscription getSubscriptionById(Long id) {
        PolicySubscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + id));
        return syncCalculatedPremium(subscription);
    }

    // ========== RENEWAL WORKFLOW METHODS ==========

    /**
     * Check and mark policies eligible for renewal (30 days before expiry)
     * This should be called by a scheduled task daily
     */
    public void updateRenewalEligibility() {
        List<PolicySubscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        
        for (PolicySubscription subscription : activeSubscriptions) {
            if (subscription.getEndDate() != null) {
                LocalDate renewalStartDate = subscription.getEndDate().minusDays(30);
                
                // Mark as eligible if within 30 days of expiry
                if (!today.isBefore(renewalStartDate) && !subscription.getRenewalEligible()) {
                    subscription.setRenewalEligible(true);
                    subscriptionRepository.save(subscription);
                }
            }
        }
    }

    /**
     * Renew an existing policy subscription
     * Maintains policy continuity and applies NCB discount
     */
    public PolicySubscription renewPolicy(Long subscriptionId) {
        PolicySubscription oldSubscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + subscriptionId));

        // Validate renewal eligibility
        if (oldSubscription.getStatus() != SubscriptionStatus.ACTIVE && 
            oldSubscription.getStatus() != SubscriptionStatus.EXPIRED) {
            throw new RuntimeException("Only ACTIVE or EXPIRED policies can be renewed. Current status: " + 
                oldSubscription.getStatus());
        }

        if (!oldSubscription.isRenewalEligible() && oldSubscription.getStatus() != SubscriptionStatus.EXPIRED) {
            throw new RuntimeException("Policy is not yet eligible for renewal. Must be within 30 days of expiry.");
        }

        // Create new subscription (renewal)
        PolicySubscription newSubscription = new PolicySubscription();
        
        // Copy customer, policy, and property
        newSubscription.setCustomer(oldSubscription.getCustomer());
        newSubscription.setPolicy(oldSubscription.getPolicy());
        newSubscription.setProperty(oldSubscription.getProperty());
        
        // Maintain policy continuity - no coverage gap
        newSubscription.setStartDate(oldSubscription.getEndDate());
        Integer durationMonths = oldSubscription.getPolicy().getDurationMonths() != null 
            ? oldSubscription.getPolicy().getDurationMonths() : 12;
        newSubscription.setEndDate(newSubscription.getStartDate().plusMonths(durationMonths));
        
        // Set renewal tracking fields
        newSubscription.setPreviousSubscriptionId(oldSubscription.getSubscriptionId());
        newSubscription.setRenewalCount(oldSubscription.getRenewalCount() + 1);
        newSubscription.setRenewalEligible(false);
        newSubscription.setRenewalReminderSent(false);
        
        // Copy risk assessment from old subscription
        newSubscription.setPropertyInspection(oldSubscription.getPropertyInspection());
        newSubscription.setRiskScore(oldSubscription.getRiskScore());
        newSubscription.setRiskMultiplier(oldSubscription.getRiskMultiplier());
        newSubscription.setBasePremiumAmount(oldSubscription.getBasePremiumAmount());
        
        // Calculate NCB discount for renewal
        boolean hadClaimsDuringPeriod = hasClaimsDuringPeriod(oldSubscription);
        
        if (!hadClaimsDuringPeriod) {
            // Increment claim-free years
            newSubscription.setClaimFreeYears(oldSubscription.getClaimFreeYears() + 1);
        } else {
            // Reset to 0 if there were claims
            newSubscription.setClaimFreeYears(0);
        }
        
        // Calculate NCB discount based on claim-free years
        Double ncbDiscount = calculateNCBDiscount(newSubscription.getClaimFreeYears());
        newSubscription.setNcbDiscount(ncbDiscount);
        newSubscription.setLastClaimDate(oldSubscription.getLastClaimDate());
        
        // Apply NCB discount to premium
        Double basePremium = oldSubscription.getPremiumAmount();
        Double renewalPremium = basePremium * (1 - ncbDiscount);
        newSubscription.setPremiumAmount(renewalPremium);
        
        // Set status to ACTIVE for renewal
        newSubscription.setStatus(SubscriptionStatus.ACTIVE);
        
        // Mark old subscription as EXPIRED if still ACTIVE
        if (oldSubscription.getStatus() == SubscriptionStatus.ACTIVE) {
            oldSubscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(oldSubscription);
        }
        
        return subscriptionRepository.save(newSubscription);
    }

    /**
     * Check if subscription had approved claims during its active period
     */
    private boolean hasClaimsDuringPeriod(PolicySubscription subscription) {
        if (subscription.getClaims() == null || subscription.getClaims().isEmpty()) {
            return false;
        }
        
        // Check if any claims were approved during the subscription period
        for (Claim claim : subscription.getClaims()) {
            if (claim.getStatus() == Claim.ClaimStatus.APPROVED) {
                return true;
            }
        }
        
        return false;
    }

    // ========== NCB (NO CLAIM BONUS) METHODS ==========

    /**
     * Calculate NCB discount based on claim-free years
     * 1 year → 10%, 2 years → 20%, 3 years → 25%, 4 years → 35%, 5+ years → 50%
     */
    public Double calculateNCBDiscount(Integer claimFreeYears) {
        if (claimFreeYears == null || claimFreeYears == 0) {
            return 0.0;
        }
        
        if (claimFreeYears >= 5) {
            return 0.50; // 50% maximum discount
        } else if (claimFreeYears == 4) {
            return 0.35; // 35% discount
        } else if (claimFreeYears == 3) {
            return 0.25; // 25% discount
        } else if (claimFreeYears == 2) {
            return 0.20; // 20% discount
        } else if (claimFreeYears == 1) {
            return 0.10; // 10% discount
        }
        
        return 0.0;
    }

    /**
     * Reset NCB when a claim is approved (called by ClaimService)
     */
    public void resetNCBForClaim(Long subscriptionId) {
        PolicySubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + subscriptionId));
        
        subscription.setClaimFreeYears(0);
        subscription.setNcbDiscount(0.0);
        subscription.setLastClaimDate(java.time.LocalDateTime.now());
        
        subscriptionRepository.save(subscription);
    }

    /**
     * Get renewal eligible subscriptions for a customer
     */
    public List<PolicySubscription> getRenewalEligibleSubscriptions(String username) {
        Customer customer = customerService.getCustomerByUsername(username);
        List<PolicySubscription> subscriptions = subscriptionRepository.findByCustomer(customer);
        
        // Filter for renewal eligible subscriptions
        return subscriptions.stream()
                .filter(sub -> sub.isRenewalEligible() && 
                       (sub.getStatus() == SubscriptionStatus.ACTIVE || 
                        sub.getStatus() == SubscriptionStatus.EXPIRED))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get NCB benefits description
     */
    public String getNCBBenefitsDescription(Integer claimFreeYears) {
        Double discount = calculateNCBDiscount(claimFreeYears);
        int discountPercentage = (int) (discount * 100);
        
        if (discountPercentage == 0) {
            return "No NCB discount. Maintain claim-free year to earn 10% discount on renewal.";
        }
        
        return String.format("You have earned %d%% NCB discount for %d claim-free year(s). " +
                "Premium reduced by ₹%.2f on renewal.", 
                discountPercentage, claimFreeYears, 0.0); // Amount calculated at renewal
    }

    /**
     * Admin assigns underwriter to a subscription
     */
    public PolicySubscription assignUnderwriter(Long subscriptionId, Long underwriterId) {
        PolicySubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + subscriptionId));

        Underwriter underwriter = underwriterRepository.findById(underwriterId)
                .orElseThrow(() -> new RuntimeException("Underwriter not found with ID: " + underwriterId));

        subscription.setUnderwriter(underwriter);
        if (subscription.getStatus() == null
                || subscription.getStatus() == SubscriptionStatus.REQUESTED
                || subscription.getStatus() == SubscriptionStatus.SUBMITTED
                || subscription.getStatus() == SubscriptionStatus.PENDING) {
            subscription.setStatus(SubscriptionStatus.UNDER_REVIEW);
        }

        return subscriptionRepository.save(subscription);
    }

    /**
     * Get all subscriptions assigned to the underwriter (based on username)
     */
    public List<PolicySubscription> getAssignedToUnderwriter(String username) {
        Underwriter underwriter = underwriterRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Underwriter not found for username: " + username));
        return subscriptionRepository.findByUnderwriter(underwriter);
    }

    public PolicySubscription syncCalculatedPremium(PolicySubscription subscription) {
        if (subscription == null || subscription.getPolicy() == null) {
            return subscription;
        }

        Double effectiveRiskScore = subscription.getRiskScore();
        if (effectiveRiskScore == null && subscription.getProperty() != null) {
            effectiveRiskScore = subscription.getProperty().getRiskScore();
        }

        if (effectiveRiskScore == null) {
            return subscription;
        }

        Inspection inspection = subscription.getPropertyInspection();
        double calculatedPremium = calculatePremium(subscription, effectiveRiskScore, inspection);
        double calculatedRiskMultiplier = getRiskMultiplier(effectiveRiskScore);

        boolean changed = false;
        if (subscription.getRiskScore() == null || Math.abs(subscription.getRiskScore() - effectiveRiskScore) > 0.0001) {
            subscription.setRiskScore(effectiveRiskScore);
            changed = true;
        }
        if (subscription.getRiskMultiplier() == null || Math.abs(subscription.getRiskMultiplier() - calculatedRiskMultiplier) > 0.0001) {
            subscription.setRiskMultiplier(calculatedRiskMultiplier);
            changed = true;
        }
        if (subscription.getPremiumAmount() == null || Math.abs(subscription.getPremiumAmount() - calculatedPremium) > 0.0001) {
            subscription.setPremiumAmount(calculatedPremium);
            changed = true;
        }

        if (changed) {
            subscription = subscriptionRepository.save(subscription);
        }

        return subscription;
    }

    private void sendPolicyEmailIfEnabled(PolicySubscription subscription, String eventKey, Map<String, String> vars) {
        if (subscription == null || subscription.getCustomer() == null || subscription.getCustomer().getUser() == null) {
            return;
        }

        User user = subscription.getCustomer().getUser();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        notificationPreferenceRepository.findByUser(user)
            .or(() -> {
                var created = notificationPreferenceRepository.save(new org.hartford.fireinsurance.model.NotificationPreference(user));
                return java.util.Optional.of(created);
            })
                .filter(pref -> Boolean.TRUE.equals(pref.getEmailEnabled()))
                .filter(pref -> pref.getEnabledEventKeys() != null && pref.getEnabledEventKeys().contains(eventKey))
                .ifPresent(pref -> emailNotificationService.sendEmailNotification(user.getEmail(), eventKey, vars));
    }
}
