package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.AssignSurveyorClaimInspectionRequest;
import org.hartford.fireinsurance.dto.AssignSurveyorPropertyInspectionRequest;
import org.hartford.fireinsurance.dto.UnderwriterRegistrationRequest;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.Document;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.Underwriter;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.hartford.fireinsurance.repository.PolicySubscriptionRepository;
import org.hartford.fireinsurance.repository.UnderwriterRepository;
import org.hartford.fireinsurance.repository.UserRepository;
import org.hartford.fireinsurance.repository.InspectionRepository;
import org.hartford.fireinsurance.repository.ClaimInspectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class UnderwriterService {

    private static final Logger log = LoggerFactory.getLogger(UnderwriterService.class);

    private final UnderwriterRepository underwriterRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PolicySubscriptionRepository subscriptionRepository;
    private final ClaimRepository claimRepository;
    private final InspectionService inspectionService;
    private final ClaimInspectionService claimInspectionService;
    private final DocumentService documentService;
    private final InspectionRepository inspectionRepository;
    private final ClaimInspectionRepository claimInspectionRepository;
    private final PolicySubscriptionService policySubscriptionService;

    public UnderwriterService(UnderwriterRepository underwriterRepository,
                              UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              PolicySubscriptionRepository subscriptionRepository,
                              ClaimRepository claimRepository,
                              InspectionService inspectionService,
                              ClaimInspectionService claimInspectionService,
                              DocumentService documentService,
                              InspectionRepository inspectionRepository,
                              ClaimInspectionRepository claimInspectionRepository,
                              PolicySubscriptionService policySubscriptionService) {
        this.underwriterRepository = underwriterRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionRepository = subscriptionRepository;
        this.claimRepository = claimRepository;
        this.inspectionService = inspectionService;
        this.claimInspectionService = claimInspectionService;
        this.documentService = documentService;
        this.inspectionRepository = inspectionRepository;
        this.claimInspectionRepository = claimInspectionRepository;
        this.policySubscriptionService = policySubscriptionService;
    }

    public Underwriter createUnderwriter(UnderwriterRegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole("UNDERWRITER");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        Underwriter underwriter = new Underwriter();
        underwriter.setUser(user);
        underwriter.setDepartment(request.getDepartment());
        underwriter.setRegion(request.getRegion());
        underwriter.setExperienceYears(request.getExperienceYears());
        underwriter.setActive(true);
        underwriter.setCreatedAt(LocalDateTime.now());

        log.info("Creating underwriter user={} region={}", request.getUsername(), request.getRegion());
        return underwriterRepository.save(underwriter);
    }

    public List<Underwriter> getAllUnderwriters() {
        return underwriterRepository.findAll();
    }

    public Underwriter getByUsername(String username) {
        return underwriterRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Underwriter not found for username: " + username));
    }

    public List<PolicySubscription> getAssignedPolicySubscriptions(String username) {
        Underwriter underwriter = underwriterRepository.findByUserUsername(username).orElse(null);
        if (underwriter == null) {
            log.warn("No underwriter profile mapped for username: {}. Returning empty subscriptions list.", username);
            return Collections.emptyList();
        }
        List<PolicySubscription> subscriptions = subscriptionRepository.findByUnderwriter(underwriter);
        
        // Proactive sync for any subscriptions that might have finished inspections but status hasn't updated
        for (PolicySubscription sub : subscriptions) {
            syncSubscriptionStatus(sub);
        }
        
        return subscriptions;
    }

    private void syncSubscriptionStatus(PolicySubscription sub) {
        // If already approved, rejected or active, skip sync
        if (sub.getStatus() == PolicySubscription.SubscriptionStatus.APPROVED || 
            sub.getStatus() == PolicySubscription.SubscriptionStatus.PAYMENT_PENDING ||
            sub.getStatus() == PolicySubscription.SubscriptionStatus.REJECTED ||
            sub.getStatus() == PolicySubscription.SubscriptionStatus.ACTIVE) {
            return;
        }

        // AGGRESSIVE SEARCH: Find any inspection that indicates work is finished
        org.hartford.fireinsurance.model.Inspection inspection = null;
        
        // 1. Check direct link
        if (sub.getPropertyInspection() != null) {
            inspection = sub.getPropertyInspection();
        }
        
        // 2. Search by Immutable IDs using specialized query (much faster than findAll)
        if (inspection == null || !isFinished(inspection)) {
            Long propId = sub.getProperty() != null ? sub.getProperty().getPropertyId() : null;
            if (propId != null) {
                List<org.hartford.fireinsurance.model.Inspection> list = inspectionRepository.findCompletedByPropertyId(propId);
                if (!list.isEmpty()) {
                    inspection = list.get(0);
                }
            }
        }
        
        // 3. Last Resort: Search specifically by subscription ID if still not found
        if (inspection == null || !isFinished(inspection)) {
            List<org.hartford.fireinsurance.model.Inspection> list = inspectionRepository.findBySubscription(sub);
            inspection = list.stream().filter(this::isFinished).findFirst().orElse(null);
        }
        
        // 4. Failsafe: Search by Address matching (Only if still not found)
        if ((inspection == null || !isFinished(inspection)) && sub.getProperty() != null && sub.getProperty().getAddress() != null) {
            List<org.hartford.fireinsurance.model.Inspection> list = inspectionRepository.findCompletedByAddress(sub.getProperty().getAddress());
            if (!list.isEmpty()) {
                inspection = list.get(0);
            }
        }

        if (inspection != null && isFinished(inspection)) {
            log.info("MASTER SYNC: Found finished inspection for sub #{}", sub.getSubscriptionId());
            
            sub.setPropertyInspection(inspection);
            
            // Sync risk data
            Double riskScore = inspection.getAssessedRiskScore();
            if (riskScore != null) {
                sub.setRiskScore(riskScore);
                policySubscriptionService.syncCalculatedPremium(sub);
            }
            
            // Force status to INSPECTED
            sub.setStatus(PolicySubscription.SubscriptionStatus.INSPECTED);
            subscriptionRepository.save(sub);
        }
    }

    private boolean isFinished(org.hartford.fireinsurance.model.Inspection i) {
        if (i == null || i.getStatus() == null) return false;
        String s = i.getStatus().name();
        // Be lenient: anything that suggests the surveyor submitted data
        return s.equalsIgnoreCase("COMPLETED") || 
               s.equalsIgnoreCase("UNDER_REVIEW") || 
               i.getAssessedRiskScore() != null;
    }

    public List<Claim> getAssignedClaims(String username) {
        Underwriter underwriter = underwriterRepository.findByUserUsername(username).orElse(null);
        if (underwriter == null) {
            log.warn("No underwriter profile mapped for username: {}. Returning empty claims list.", username);
            return Collections.emptyList();
        }
        List<Claim> claims = claimRepository.findByUnderwriter(underwriter);
        
        // Proactive sync for claims
        for (Claim claim : claims) {
            syncClaimStatus(claim);
        }
        
        return claims;
    }

    private void syncClaimStatus(Claim claim) {
        if (claim.getStatus() == Claim.ClaimStatus.APPROVED || 
            claim.getStatus() == Claim.ClaimStatus.REJECTED ||
            claim.getStatus() == Claim.ClaimStatus.SETTLED) {
            return;
        }

        org.hartford.fireinsurance.model.ClaimInspection inspection = null;
        
        // Match by Link or ID
        if (claim.getClaimInspection() != null) {
            inspection = claim.getClaimInspection();
        } else {
            List<org.hartford.fireinsurance.model.ClaimInspection> list = claimInspectionRepository.findCompletedByClaimId(claim.getClaimId());
            if (!list.isEmpty()) {
                inspection = list.get(0);
            }
        }
        
        if (inspection != null && isClaimInspFinished(inspection)) {
            log.info("MASTER SYNC: Found finished inspection for claim #{}", claim.getClaimId());
            claim.setStatus(Claim.ClaimStatus.SURVEY_COMPLETED);
            claimRepository.save(claim);
        }
    }

    private boolean isClaimInspFinished(org.hartford.fireinsurance.model.ClaimInspection i) {
        if (i == null || i.getStatus() == null) return false;
        String s = i.getStatus().name();
        return s.equalsIgnoreCase("UNDER_REVIEW") || 
               s.equalsIgnoreCase("APPROVED") ||
               i.getEstimatedLoss() != null;
    }

    public PolicySubscription assignSurveyorForPropertyInspection(String username,
                                                                  AssignSurveyorPropertyInspectionRequest request) {
        Underwriter underwriter = getByUsername(username);
        PolicySubscription subscription = subscriptionRepository.findById(request.getSubscriptionId())
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + request.getSubscriptionId()));

        if (subscription.getUnderwriter() == null || !subscription.getUnderwriter().getUnderwriterId().equals(underwriter.getUnderwriterId())) {
            throw new RuntimeException("Unauthorized: subscription is not assigned to this underwriter");
        }

        org.hartford.fireinsurance.model.Inspection inspection = inspectionService.assignSurveyorForSubscription(request.getSubscriptionId(), request.getSurveyorId());
        subscription.setPropertyInspection(inspection);
        subscription.setStatus(PolicySubscription.SubscriptionStatus.INSPECTION_PENDING);
        return subscriptionRepository.save(subscription);
    }

    public Claim assignSurveyorForClaimInspection(String username, AssignSurveyorClaimInspectionRequest request) {
        Underwriter underwriter = getByUsername(username);
        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + request.getClaimId()));

        if (claim.getUnderwriter() == null || !claim.getUnderwriter().getUnderwriterId().equals(underwriter.getUnderwriterId())) {
            throw new RuntimeException("Unauthorized: claim is not assigned to this underwriter");
        }

        claimInspectionService.assignSurveyor(request.getClaimId(), request.getSurveyorId());
        claim.setStatus(Claim.ClaimStatus.SURVEYOR_ASSIGNED);
        return claimRepository.save(claim);
    }

    public PolicySubscription approvePolicySubscription(String username, Long subscriptionId) {
        Underwriter underwriter = getByUsername(username);
        PolicySubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + subscriptionId));

        if (subscription.getUnderwriter() == null || !subscription.getUnderwriter().getUnderwriterId().equals(underwriter.getUnderwriterId())) {
            throw new RuntimeException("Unauthorized: subscription is not assigned to this underwriter");
        }

        subscription.setStatus(PolicySubscription.SubscriptionStatus.PAYMENT_PENDING);
        subscription.setPaymentReceived(false);
        return subscriptionRepository.save(subscription);
    }

    public PolicySubscription rejectPolicySubscription(String username, Long subscriptionId) {
        Underwriter underwriter = getByUsername(username);
        PolicySubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + subscriptionId));

        if (subscription.getUnderwriter() == null || !subscription.getUnderwriter().getUnderwriterId().equals(underwriter.getUnderwriterId())) {
            throw new RuntimeException("Unauthorized: subscription is not assigned to this underwriter");
        }

        subscription.setStatus(PolicySubscription.SubscriptionStatus.REJECTED);
        return subscriptionRepository.save(subscription);
    }

    public Claim approveClaim(String username, Long claimId) {
        Underwriter underwriter = getByUsername(username);
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));

        if (claim.getUnderwriter() == null || !claim.getUnderwriter().getUnderwriterId().equals(underwriter.getUnderwriterId())) {
            throw new RuntimeException("Unauthorized: claim is not assigned to this underwriter");
        }

        // Only allow approval if claim is in UNDER_REVIEW (not SIU_CLEARED)
        if (claim.getStatus() != Claim.ClaimStatus.UNDER_REVIEW) {
            throw new RuntimeException("Claim must be in UNDER_REVIEW status to be approved by underwriter.");
        }

        claim.setStatus(Claim.ClaimStatus.APPROVED);
        return claimRepository.save(claim);
    }

    public Claim rejectClaim(String username, Long claimId) {
        Underwriter underwriter = getByUsername(username);
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));

        if (claim.getUnderwriter() == null || !claim.getUnderwriter().getUnderwriterId().equals(underwriter.getUnderwriterId())) {
            throw new RuntimeException("Unauthorized: claim is not assigned to this underwriter");
        }

        // Only allow rejection if claim is in UNDER_REVIEW (not SIU_CLEARED)
        if (claim.getStatus() != Claim.ClaimStatus.UNDER_REVIEW) {
            throw new RuntimeException("Claim must be in UNDER_REVIEW status to be rejected by underwriter.");
        }

        claim.setStatus(Claim.ClaimStatus.REJECTED);
        return claimRepository.save(claim);
    }

    public List<Document> getSubscriptionDocuments(String username, Long subscriptionId) {
        Underwriter underwriter = getByUsername(username);
        PolicySubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + subscriptionId));

        if (subscription.getUnderwriter() == null || !subscription.getUnderwriter().getUnderwriterId().equals(underwriter.getUnderwriterId())) {
            throw new RuntimeException("Unauthorized: subscription is not assigned to this underwriter");
        }

        return documentService.getDocumentsByProperty(subscription.getProperty().getPropertyId());
    }

    public List<Document> getClaimDocuments(String username, Long claimId) {
        Underwriter underwriter = getByUsername(username);
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));

        if (claim.getUnderwriter() == null || !claim.getUnderwriter().getUnderwriterId().equals(underwriter.getUnderwriterId())) {
            throw new RuntimeException("Unauthorized: claim is not assigned to this underwriter");
        }

        return documentService.getDocumentsByClaim(claimId);
    }
}

