package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.InspectionRequest;
import org.hartford.fireinsurance.model.Inspection;
import org.hartford.fireinsurance.model.Inspection.InspectionStatus;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.repository.InspectionRepository;
import org.hartford.fireinsurance.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final PropertyService propertyService;
    private final SurveyorService surveyorService;
    private final org.hartford.fireinsurance.repository.PolicySubscriptionRepository subscriptionRepository;
    private final PremiumCalculationService premiumCalculationService;
    private final EmailNotificationService emailNotificationService;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private static final Logger log = LoggerFactory.getLogger(InspectionService.class);

    public InspectionService(
            InspectionRepository inspectionRepository,
            PropertyService propertyService,
            SurveyorService surveyorService,
            org.hartford.fireinsurance.repository.PolicySubscriptionRepository subscriptionRepository,
            PremiumCalculationService premiumCalculationService,
            EmailNotificationService emailNotificationService,
            NotificationPreferenceRepository notificationPreferenceRepository) {
        this.inspectionRepository = inspectionRepository;
        this.propertyService = propertyService;
        this.surveyorService = surveyorService;
        this.subscriptionRepository = subscriptionRepository;
        this.premiumCalculationService = premiumCalculationService;
        this.emailNotificationService = emailNotificationService;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    /**
     * Admin assigns surveyor to property for underwriting inspection.
     * Sends email notification to surveyor.
     */
    public Inspection assignSurveyor(Long propertyId, Long surveyorId) {
        Property property = propertyService.getPropertyById(propertyId);
        Surveyor surveyor = surveyorService.getSurveyor(surveyorId);

        Inspection inspection = new Inspection();
        inspection.setProperty(property);
        inspection.setSurveyor(surveyor);
        inspection.setStatus(InspectionStatus.ASSIGNED);

        Inspection savedInspection = inspectionRepository.save(inspection);

        // Auto-link inspection to any REQUESTED or PENDING or INSPECTION_PENDING subscription for this property
        List<org.hartford.fireinsurance.model.PolicySubscription> subscriptions = subscriptionRepository.findByProperty(property);
        for (org.hartford.fireinsurance.model.PolicySubscription sub : subscriptions) {
            if (sub.getStatus() == org.hartford.fireinsurance.model.PolicySubscription.SubscriptionStatus.REQUESTED ||
                sub.getStatus() == org.hartford.fireinsurance.model.PolicySubscription.SubscriptionStatus.PENDING ||
                sub.getStatus() == org.hartford.fireinsurance.model.PolicySubscription.SubscriptionStatus.INSPECTION_PENDING) {
                sub.setPropertyInspection(savedInspection);
                subscriptionRepository.save(sub);
            }
        }
        
        // Send email notification to surveyor
        sendInspectionAssignedEmail(savedInspection, surveyor);

        return savedInspection;
    }

    /**
     * Surveyor submits property inspection report
     * CRITICAL: This triggers premium calculation based on risk score
     */
    public Inspection submitInspection(Long inspectionId, String username, InspectionRequest request) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("Inspection not found with ID: " + inspectionId));

        // Security check: inspection must belong to surveyor
        if (!inspection.getSurveyor().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized: Inspection does not belong to this surveyor");
        }

        // Validate risk score range
        Double riskScore = request.getAssessedRiskScore();
        if (riskScore < 0 || riskScore > 10) {
            throw new RuntimeException("Risk score must be between 0 and 10. Provided: " + riskScore);
        }

        // Update inspection
        inspection.setAssessedRiskScore(riskScore);
        inspection.setRemarks(request.getRemarks());
        if (request.getInspectionReport() != null) {
            inspection.setInspectionReport(request.getInspectionReport());
        }
        if (request.getRecommendedCoverage() != null) {
            inspection.setRecommendedCoverage(request.getRecommendedCoverage());
        }
        inspection.setFireSafetyAvailable(request.getFireSafetyAvailable());
        inspection.setSprinklerSystem(request.getSprinklerSystem());
        inspection.setFireExtinguishers(request.getFireExtinguishers());
        inspection.setDistanceFromFireStation(request.getDistanceFromFireStation());
        inspection.setConstructionRisk(request.getConstructionRisk());
        inspection.setHazardRisk(request.getHazardRisk());
        inspection.setRecommendedPremium(request.getRecommendedPremium());
        
        // Expanded COPE fields
        inspection.setConstructionType(request.getConstructionType());
        inspection.setRoofType(request.getRoofType());
        inspection.setOccupancyType(request.getOccupancyType());
        inspection.setElectricalAuditStatus(request.getElectricalAuditStatus());
        inspection.setHazardousMaterialsPresent(request.getHazardousMaterialsPresent());
        inspection.setAdjacentBuildingDistance(request.getAdjacentBuildingDistance());
        inspection.setInternalProtectionNotes(request.getInternalProtectionNotes());

        inspection.setInspectionDate(LocalDateTime.now());
        inspection.setStatus(InspectionStatus.COMPLETED);

        // Update property risk score
        inspection.getProperty().setRiskScore(riskScore);

        Inspection savedInspection = inspectionRepository.save(inspection);

        // Update related PolicySubscription(s)
        // 1. Check direct link in the Inspection object itself (most reliable)
        if (inspection.getSubscription() != null) {
            updateSubscriptionState(inspection.getSubscription(), savedInspection, riskScore);
        }

        // 2. Fallback: Update other subscriptions for the same property
        List<org.hartford.fireinsurance.model.PolicySubscription> subscriptions = subscriptionRepository.findByProperty(inspection.getProperty());
        for (org.hartford.fireinsurance.model.PolicySubscription sub : subscriptions) {
            // Check by Property ID directly if lazy loading is an issue
            boolean sameProperty = sub.getProperty() != null && sub.getProperty().getPropertyId().equals(inspection.getProperty().getPropertyId());
            boolean sameInspection = sub.getPropertyInspection() != null && sub.getPropertyInspection().getInspectionId().equals(savedInspection.getInspectionId());
            
            if (sameProperty || sameInspection || sub.getSubscriptionId().equals(inspection.getSubscription() != null ? inspection.getSubscription().getSubscriptionId() : null)) {
                if (sub.getStatus() != org.hartford.fireinsurance.model.PolicySubscription.SubscriptionStatus.INSPECTED) {
                    updateSubscriptionState(sub, savedInspection, riskScore);
                }
            }
        }
        
        // Send email notification to customer that inspection is completed
        sendInspectionCompletedEmail(savedInspection);

        return savedInspection;
    }

    private void updateSubscriptionState(org.hartford.fireinsurance.model.PolicySubscription sub, Inspection inspection, Double riskScore) {
        sub.setPropertyInspection(inspection);
        
        // Calculate premium based on risk score
        if (sub.getBasePremiumAmount() != null) {
            double riskMultiplier = premiumCalculationService.getRiskMultiplier(riskScore);
            double calculatedPremium = premiumCalculationService.calculatePremium(sub, riskScore, inspection);
            
            sub.setRiskScore(riskScore);
            sub.setRiskMultiplier(riskMultiplier);
            sub.setPremiumAmount(calculatedPremium);
            sub.setStatus(org.hartford.fireinsurance.model.PolicySubscription.SubscriptionStatus.INSPECTED);
        } else {
            sub.setStatus(org.hartford.fireinsurance.model.PolicySubscription.SubscriptionStatus.INSPECTED);
        }

        subscriptionRepository.save(sub);
    }

    /**
     * Get all inspections for a surveyor by username
     */
    public List<Inspection> getBySurveyor(String username) {
        Surveyor surveyor = surveyorService.getSurveyorByUsername(username);
        return inspectionRepository.findBySurveyor(surveyor);
    }

    /**
     * Get all inspections (Admin only)
     */
    public List<Inspection> getAll() {
        return inspectionRepository.findAll();
    }

    /**
     * Get inspection by ID
     */
    public Inspection getById(Long id) {
        return inspectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inspection not found with ID: " + id));
    }

    /**
     * Admin assigns surveyor to subscription for underwriting inspection
     */
    public Inspection assignSurveyorForSubscription(Long subscriptionId, Long surveyorId) {
        org.hartford.fireinsurance.model.PolicySubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + subscriptionId));

        Inspection inspection = assignSurveyor(subscription.getProperty().getPropertyId(), surveyorId);
        inspection.setSubscription(subscription);
        inspection.setStatus(InspectionStatus.ASSIGNED);
        
        // Ensure bidirectional link
        subscription.setPropertyInspection(inspection);
        subscriptionRepository.save(subscription);
        
        return inspectionRepository.save(inspection);
    }

    /**
     * Helper: Send email notification when inspection is assigned to surveyor.
     * Non-blocking: errors are logged but don't fail the transaction.
     */
    private void sendInspectionAssignedEmail(Inspection inspection, Surveyor surveyor) {
        try {
            if (surveyor == null || surveyor.getUser() == null || surveyor.getUser().getEmail() == null) {
                log.warn("Cannot send inspection assigned email: missing surveyor or email for inspection {}", inspection.getInspectionId());
                return;
            }

            // Check user preferences
            notificationPreferenceRepository.findByUser(surveyor.getUser())
                    .filter(pref -> pref.getEmailEnabled() && pref.getEnabledEventKeys().contains("INSPECTION_ASSIGNED"))
                    .ifPresent(pref -> {
                        Map<String, String> vars = new HashMap<>();
                        vars.put("inspectionId", inspection.getInspectionId() != null ? inspection.getInspectionId().toString() : "Unknown");
                        vars.put("surveyorName", surveyor.getUser().getUsername());
                        vars.put("propertyAddress", inspection.getProperty() != null ? inspection.getProperty().getAddress() : "Unknown");
                        vars.put("scheduledDate", "To be determined");
                        
                        emailNotificationService.sendEmailNotification(
                                surveyor.getUser().getEmail(),
                                "INSPECTION_ASSIGNED",
                                vars
                        );
                    });
        } catch (Exception e) {
            // Non-blocking: log but don't throw
            log.error("Error sending inspection assigned email for inspection {}: {}", inspection.getInspectionId(), e.getMessage());
        }
    }

    /**
     * Helper: Send email notification when inspection is completed.
     * Non-blocking: errors are logged but don't fail the transaction.
     */
    private void sendInspectionCompletedEmail(Inspection inspection) {
        try {
            if (inspection.getSubscription() == null) {
                log.warn("Cannot send inspection completed email: no subscription linked for inspection {}", inspection.getInspectionId());
                return;
            }

            org.hartford.fireinsurance.model.Customer customer = inspection.getSubscription().getCustomer();
            if (customer == null || customer.getUser() == null || customer.getUser().getEmail() == null) {
                log.warn("Cannot send inspection completed email: missing customer or email for inspection {}", inspection.getInspectionId());
                return;
            }

            // Check user preferences
            notificationPreferenceRepository.findByUser(customer.getUser())
                    .filter(pref -> pref.getEmailEnabled() && pref.getEnabledEventKeys().contains("INSPECTION_COMPLETED"))
                    .ifPresent(pref -> {
                        Map<String, String> vars = new HashMap<>();
                        vars.put("inspectionId", inspection.getInspectionId() != null ? inspection.getInspectionId().toString() : "Unknown");
                        vars.put("customerName", customer.getUser().getUsername());
                        vars.put("surveyorName", inspection.getSurveyor() != null ? inspection.getSurveyor().getUser().getUsername() : "Inspector");
                        vars.put("completionDate", LocalDateTime.now().toString());
                        
                        emailNotificationService.sendEmailNotification(
                                customer.getUser().getEmail(),
                                "INSPECTION_COMPLETED",
                                vars
                        );
                    });
        } catch (Exception e) {
            // Non-blocking: log but don't throw
            log.error("Error sending inspection completed email for inspection {}: {}", inspection.getInspectionId(), e.getMessage());
        }
    }
}
