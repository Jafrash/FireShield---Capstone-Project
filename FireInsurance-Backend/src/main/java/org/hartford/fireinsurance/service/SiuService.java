package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.SiuClaimDto;
import org.hartford.fireinsurance.dto.SiuClaimsResponse;
import org.hartford.fireinsurance.dto.SiuInvestigationActionRequest;
import org.hartford.fireinsurance.dto.SiuInvestigationActionResponse;
import org.hartford.fireinsurance.dto.SiuInvestigatorRegistrationRequest;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.SiuInvestigator;
import org.hartford.fireinsurance.model.Underwriter;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.SiuInvestigatorRepository;
import org.hartford.fireinsurance.repository.UserRepository;
import org.hartford.fireinsurance.repository.UnderwriterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for SIU (Special Investigation Unit) operations.
 * Handles fraud detection, suspicious claim identification, and investigation workflow.
 */
@Service
public class SiuService {

    private static final Logger log = LoggerFactory.getLogger(SiuService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final ClaimService claimService;
    private final SiuInvestigatorRepository siuInvestigatorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UnderwriterRepository underwriterRepository;

    public SiuService(ClaimService claimService,
                      SiuInvestigatorRepository siuInvestigatorRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      UnderwriterRepository underwriterRepository) {
        this.claimService = claimService;
        this.siuInvestigatorRepository = siuInvestigatorRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.underwriterRepository = underwriterRepository;
    }

    /**
     * Get all claims assigned for SIU investigation with aggregated statistics.
     * Returns claims that require fraud investigation based on various criteria.
     */
    public SiuClaimsResponse getSiuClaims() {
        log.info("Fetching SIU claims for investigation dashboard");

        // Get all claims from the system
        List<Claim> allClaims = claimService.getAllClaims();

        // Filter and transform claims for SIU investigation
        List<SiuClaimDto> siuClaims = allClaims.stream()
                .filter(this::requiresSiuInvestigation)
                .map(this::mapToSiuClaimDto)
                .collect(Collectors.toList());

        // Calculate aggregated statistics
        int totalCount = siuClaims.size();
        int highPriorityCount = (int) siuClaims.stream()
                .filter(claim -> SiuClaimDto.Priority.HIGH.equals(claim.getPriority()))
                .count();
        int suspiciousCount = (int) siuClaims.stream()
                .filter(claim -> claim.getFraudScore() >= 70)
                .count();

        double averageFraudScore = siuClaims.stream()
                .mapToDouble(SiuClaimDto::getFraudScore)
                .average()
                .orElse(0.0);

        double totalAmount = siuClaims.stream()
                .mapToDouble(claim -> claim.getClaimAmount() != null ? claim.getClaimAmount() : 0.0)
                .sum();

        log.info("SIU Claims Summary: Total={}, HighPriority={}, Suspicious={}, AvgFraudScore={:.1f}%, TotalAmount=${:.0f}",
                totalCount, highPriorityCount, suspiciousCount, averageFraudScore, totalAmount);

        return new SiuClaimsResponse(siuClaims, totalCount, highPriorityCount,
                                   suspiciousCount, Math.round(averageFraudScore), totalAmount);
    }

    /**
     * Get detailed claim information for SIU investigation.
     * Returns comprehensive claim data including customer, policy, and property information.
     */
    public Object getClaimDetailsForInvestigation(String claimId) {
        try {
            log.info("Fetching detailed claim information for SIU investigation: {}", claimId);

            Long claimIdLong = Long.parseLong(claimId);
            Claim claim = claimService.getClaimById(claimIdLong);

            if (claim == null) {
                log.warn("Claim not found for SIU investigation: {}", claimId);
                return null;
            }

            // Build detailed claim response matching frontend interface
            return buildSiuClaimDetailsResponse(claim);

        } catch (NumberFormatException e) {
            log.error("Invalid claim ID format for SIU investigation: {}", claimId);
            return null;
        } catch (Exception e) {
            log.error("Error fetching claim details for SIU investigation {}: {}", claimId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Build detailed claim response matching frontend SiuClaimDetails interface
     */
    private Object buildSiuClaimDetailsResponse(Claim claim) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();

        // Basic claim information
        response.put("claimId", claim.getClaimId() != null ? claim.getClaimId().toString() : "");
        response.put("description", claim.getDescription() != null ? claim.getDescription() : "Fire damage claim");
        response.put("claimAmount", claim.getClaimAmount() != null ? claim.getClaimAmount() : 0.0);
        response.put("status", claim.getStatus() != null ? claim.getStatus().name() : "SUBMITTED");
        response.put("createdAt", claim.getCreatedAt() != null ? claim.getCreatedAt().toString() : "");
        response.put("estimatedLoss", claim.getEstimatedLoss() != null ? claim.getEstimatedLoss() : 0.0);
        response.put("calculatedDeductible", 0.0); // Not stored in current model, using default
        response.put("calculatedDepreciation", 0.0); // Not stored in current model, using default
        response.put("settlementAmount", claim.getSettlementAmount() != null ? claim.getSettlementAmount() : 0.0);
        response.put("underwriterId", claim.getUnderwriter() != null ? claim.getUnderwriter().getUnderwriterId() : null);
        response.put("fraudScore", claim.getFraudScore() != null ? claim.getFraudScore() : 30.0);
        response.put("incidentDate", claim.getIncidentDate() != null ? claim.getIncidentDate().toString() : "");

        // Fire-specific details
        response.put("causeOfFire", claim.getCauseOfFire() != null ? claim.getCauseOfFire() : "Electrical fault");
        response.put("firNumber", claim.getFirNumber());
        response.put("fireBrigadeReportNumber", claim.getFireBrigadeReportNumber());
        response.put("salvageDetails", claim.getSalvageDetails());

        // Customer information
        if (claim.getSubscription() != null && claim.getSubscription().getCustomer() != null) {
            Customer customer = claim.getSubscription().getCustomer();
            java.util.Map<String, Object> customerInfo = new java.util.HashMap<>();

            customerInfo.put("customerId", customer.getCustomerId() != null ? customer.getCustomerId() : 0);
            // Use username as first name since separate first/last name fields don't exist
            customerInfo.put("firstName", customer.getUser() != null ? customer.getUser().getUsername() : "");
            customerInfo.put("lastName", ""); // Not available in current model
            customerInfo.put("email", customer.getUser() != null ? customer.getUser().getEmail() : "");
            customerInfo.put("phoneNumber", customer.getUser() != null ? customer.getUser().getPhoneNumber() : "");
            customerInfo.put("address", customer.getAddress() != null ? customer.getAddress() : "");
            customerInfo.put("city", customer.getCity() != null ? customer.getCity() : "");
            customerInfo.put("state", customer.getState() != null ? customer.getState() : "");

            response.put("customer", customerInfo);

            // Policy information
            if (claim.getSubscription().getPolicy() != null) {
                PolicySubscription subscription = claim.getSubscription();
                java.util.Map<String, Object> policyInfo = new java.util.HashMap<>();

                policyInfo.put("policyId", subscription.getPolicy().getPolicyId() != null ? subscription.getPolicy().getPolicyId() : 0);
                policyInfo.put("policyName", subscription.getPolicy().getPolicyName() != null ? subscription.getPolicy().getPolicyName() : "Fire Insurance");
                policyInfo.put("policyType", "FIRE"); // Default since policyType field doesn't exist
                policyInfo.put("maxCoverageAmount", subscription.getPolicy().getMaxCoverageAmount() != null ? subscription.getPolicy().getMaxCoverageAmount() : 1000000.0);
                policyInfo.put("deductible", subscription.getPolicy().getDeductible() != null ? subscription.getPolicy().getDeductible() : 10000.0);

                response.put("policy", policyInfo);
            }

            // Property information if available
            if (claim.getSubscription().getProperty() != null) {
                java.util.Map<String, Object> propertyInfo = new java.util.HashMap<>();

                propertyInfo.put("propertyId", claim.getSubscription().getProperty().getPropertyId() != null ? claim.getSubscription().getProperty().getPropertyId() : 0);
                propertyInfo.put("propertyType", claim.getSubscription().getProperty().getPropertyType() != null ? claim.getSubscription().getProperty().getPropertyType() : "Residential");
                propertyInfo.put("address", claim.getSubscription().getProperty().getAddress() != null ? claim.getSubscription().getProperty().getAddress() : "");
                propertyInfo.put("propertyValue", 500000.0); // Default since propertyValue field doesn't exist
                propertyInfo.put("propertyAge", claim.getSubscription().getProperty().getPropertyAge());

                response.put("property", propertyInfo);
            }
        }

        response.put("subscriptionId", claim.getSubscription() != null ? claim.getSubscription().getSubscriptionId() : null);

        log.info("Built detailed SIU claim response for claim: {}", claim.getClaimId());
        return response;
    }

    /**
     * Determine if a claim requires SIU investigation based on various risk factors.
     */
    private boolean requiresSiuInvestigation(Claim claim) {
        // Only show HIGH risk or high fraud score claims (>= 70)
        return (claim.getRiskLevel() != null && claim.getRiskLevel().name().equals("HIGH"))
            || (claim.getFraudScore() != null && claim.getFraudScore() >= 70.0);
    }

    /**
     * Check for additional high-risk indicators that warrant SIU investigation.
     */
    private boolean isHighRiskClaim(Claim claim) {
        // Check for multiple claims from same customer (potential pattern fraud)
        try {
            if (claim.getSubscription() != null && claim.getSubscription().getCustomer() != null) {
                Customer customer = claim.getSubscription().getCustomer();
                List<Claim> customerClaims = claimService.getClaimsByUsername(customer.getUser().getUsername());

                // Flag if customer has multiple claims in short period
                long recentClaims = customerClaims.stream()
                        .filter(c -> c.getCreatedAt() != null &&
                                   c.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusMonths(12)))
                        .count();

                if (recentClaims >= 3) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Error checking customer claim history for SIU risk assessment: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Convert Claim entity to SiuClaimDto for investigation dashboard.
     */
    private SiuClaimDto mapToSiuClaimDto(Claim claim) {
        SiuClaimDto dto = new SiuClaimDto();

        // Basic claim information
        dto.setClaimId(claim.getClaimId() != null ? claim.getClaimId().toString() : "N/A");
        dto.setFraudScore(claim.getFraudScore() != null ? claim.getFraudScore() : 0.0);
        dto.setState(mapClaimStatusToSiuState(claim.getStatus()));
        dto.setClaimAmount(claim.getClaimAmount());

        // Policy and customer information
        try {
            PolicySubscription subscription = claim.getSubscription();
            if (subscription != null) {
                if (subscription.getPolicy() != null) {
                    dto.setPolicyNumber(subscription.getPolicy().getPolicyId() != null ?
                                       subscription.getPolicy().getPolicyId().toString() : null);
                }

                if (subscription.getCustomer() != null && subscription.getCustomer().getUser() != null) {
                    dto.setCustomerName(subscription.getCustomer().getUser().getUsername());
                }
            }
        } catch (Exception e) {
            log.warn("Error mapping policy/customer info for claim {}: {}", claim.getClaimId(), e.getMessage());
        }

        // Investigation priority based on fraud score and amount
        dto.setPriority(calculateInvestigationPriority(claim));

        // Timeline information
        if (claim.getCreatedAt() != null) {
            dto.setAssignedDate(claim.getCreatedAt().format(DATE_FORMATTER));
            dto.setLastActivity(claim.getCreatedAt().format(DATE_FORMATTER));
        }

        // Suspicious indicators based on fraud score and claim characteristics
        dto.setSuspiciousIndicators(generateSuspiciousIndicators(claim));

        return dto;
    }

    /**
     * Map internal claim status to SIU investigation states.
     */
    private String mapClaimStatusToSiuState(Claim.ClaimStatus status) {
        if (status == null) {
            return "PENDING_REVIEW";
        }

        switch (status) {
            case SUBMITTED:
                return "PENDING_REVIEW";
            case UNDER_REVIEW:
                return "UNDER_INVESTIGATION";
            case SURVEYOR_ASSIGNED:
                return "SURVEYOR_ASSIGNED";
            case SURVEY_COMPLETED:
                return "UNDER_INVESTIGATION";
            case APPROVED:
                return "CLEARED";
            case REJECTED:
                return "FRAUD_CONFIRMED";
            default:
                return "PENDING_REVIEW";
        }
    }

    /**
     * Calculate investigation priority based on fraud score, claim amount, and risk factors.
     */
    private SiuClaimDto.Priority calculateInvestigationPriority(Claim claim) {
        double fraudScore = claim.getFraudScore() != null ? claim.getFraudScore() : 0.0;
        Double claimAmount = claim.getClaimAmount();

        // High priority: High fraud score or very high claim amounts
        if (fraudScore >= 80.0 || (claimAmount != null && claimAmount > 500000.0)) {
            return SiuClaimDto.Priority.HIGH;
        }

        // Medium priority: Moderate fraud score or high claim amounts
        if (fraudScore >= 60.0 || (claimAmount != null && claimAmount > 200000.0)) {
            return SiuClaimDto.Priority.MEDIUM;
        }

        // Low priority: Everything else requiring investigation
        return SiuClaimDto.Priority.LOW;
    }

    /**
     * Generate suspicious indicators list based on claim characteristics.
     */
    private List<String> generateSuspiciousIndicators(Claim claim) {
        // This is a simplified implementation. In a real system, this would be
        // based on complex fraud detection algorithms and historical patterns.

        double fraudScore = claim.getFraudScore() != null ? claim.getFraudScore() : 0.0;

        if (fraudScore >= 80.0) {
            return Arrays.asList("High fraud score", "Inconsistent documentation", "Suspicious timing");
        } else if (fraudScore >= 60.0) {
            return Arrays.asList("Elevated fraud score", "Multiple recent claims");
        } else if (fraudScore >= 40.0) {
            return Arrays.asList("Moderate risk indicators");
        } else {
            return Arrays.asList("Routine investigation");
        }
    }

    // === SIU Investigation Actions ===

    /**
     * Start formal SIU investigation on a claim.
     * Changes status to UNDER_REVIEW to indicate active investigation.
     */
    public SiuInvestigationActionResponse startInvestigation(SiuInvestigationActionRequest request, String investigatorUsername) {
        try {
            log.info("Starting SIU investigation for claim {} by investigator {}", request.getClaimId(), investigatorUsername);

            Long claimId = Long.parseLong(request.getClaimId());
            Claim updatedClaim = claimService.updateClaimStatus(claimId, Claim.ClaimStatus.UNDER_REVIEW);

            log.info("Successfully started investigation for claim {} - Status changed to UNDER_REVIEW", claimId);

            return SiuInvestigationActionResponse.success(
                request.getClaimId(),
                "UNDER_REVIEW",
                "Investigation started successfully. Claim is now under active SIU review."
            );

        } catch (NumberFormatException e) {
            log.error("Invalid claim ID format: {}", request.getClaimId());
            return SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "Invalid claim ID format. Must be a valid number."
            );
        } catch (RuntimeException e) {
            log.error("Error starting investigation for claim {}: {}", request.getClaimId(), e.getMessage());
            return SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "Failed to start investigation: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error starting investigation for claim {}: {}", request.getClaimId(), e.getMessage(), e);
            return SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "System error occurred while starting investigation. Please try again."
            );
        }
    }

    /**
     * Mark claim as fraudulent after investigation.
     * Changes status to REJECTED to indicate fraud confirmed.
     */
    public SiuInvestigationActionResponse markAsFraud(SiuInvestigationActionRequest request, String investigatorUsername) {
        try {
            log.info("Marking claim {} as fraudulent by investigator {}", request.getClaimId(), investigatorUsername);

            Long claimId = Long.parseLong(request.getClaimId());
            Claim updatedClaim = claimService.updateClaimStatus(claimId, Claim.ClaimStatus.REJECTED);

            log.info("Successfully marked claim {} as fraudulent - Status changed to REJECTED", claimId);

            return SiuInvestigationActionResponse.success(
                request.getClaimId(),
                "REJECTED",
                "Claim successfully marked as fraudulent. Investigation closed with fraud confirmed."
            );

        } catch (NumberFormatException e) {
            log.error("Invalid claim ID format: {}", request.getClaimId());
            return SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "Invalid claim ID format. Must be a valid number."
            );
        } catch (RuntimeException e) {
            log.error("Error marking claim {} as fraud: {}", request.getClaimId(), e.getMessage());
            return SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "Failed to mark claim as fraudulent: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error marking claim {} as fraud: {}", request.getClaimId(), e.getMessage(), e);
            return SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "System error occurred while processing fraud marking. Please try again."
            );
        }
    }

    /**
     * Clear claim as legitimate after investigation.
     * Changes status to APPROVED to indicate investigation completed and claim is valid.
     */
    public SiuInvestigationActionResponse clearClaim(SiuInvestigationActionRequest request, String investigatorUsername) {
        try {
            log.info("Clearing claim {} as legitimate by investigator {}", request.getClaimId(), investigatorUsername);

            Long claimId = Long.parseLong(request.getClaimId());
            Claim claim = claimService.getClaimById(claimId);
            if (claim == null) {
                throw new RuntimeException("Claim not found with ID: " + claimId);
            }

            // Find an available underwriter (example: first one)
            Underwriter underwriter = underwriterRepository.findAll().stream().findFirst().orElse(null);
            if (underwriter == null) {
                throw new RuntimeException("No underwriter available for assignment");
            }

            // Set status to SIU_CLEARED, not APPROVED or UNDER_REVIEW
            claim.setStatus(Claim.ClaimStatus.SIU_CLEARED);
            claim.setSiuStatus("CLEARED");
            claim.setUnderwriter(underwriter);
            claimService.saveClaim(claim);

            log.info("Successfully cleared claim {} as legitimate - Status changed to SIU_CLEARED, assigned to underwriter {}", claimId, underwriter.getUnderwriterId());

            return SiuInvestigationActionResponse.success(
                request.getClaimId(),
                "SIU_CLEARED",
                "Claim successfully cleared as legitimate. Investigation closed with no fraud detected. Sent to underwriter for review."
            );
        } catch (NumberFormatException e) {
            log.error("Invalid claim ID format: {}", request.getClaimId());
            return SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "Invalid claim ID format. Must be a valid number."
            );
        } catch (RuntimeException e) {
            log.error("Error clearing claim {}: {}", request.getClaimId(), e.getMessage());
            return SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "Failed to clear claim: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error clearing claim {}: {}", request.getClaimId(), e.getMessage(), e);
            return SiuInvestigationActionResponse.failure(
                request.getClaimId(),
                "System error occurred while processing claim clearance. Please try again."
            );
        }
    }

    // === SIU Investigator Management ===

    @Transactional
    public SiuInvestigator createSiuInvestigator(SiuInvestigatorRegistrationRequest request) {
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
        user.setPhoneNumber(request.getPhone());
        user.setRole("SIU_INVESTIGATOR");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        SiuInvestigator investigator = new SiuInvestigator();
        investigator.setUser(user);
        investigator.setFirstName(request.getFirstName());
        investigator.setLastName(request.getLastName());
        investigator.setBadgeNumber(request.getBadgeNumber());
        investigator.setDepartment(request.getDepartment());
        investigator.setExperienceYears(request.getExperienceYears());
        investigator.setSpecialization(request.getSpecialization());
        investigator.setActive(true);
        investigator.setCreatedAt(LocalDateTime.now());

        log.info("Creating SIU investigator user={} department={}", request.getUsername(), request.getDepartment());
        return siuInvestigatorRepository.save(investigator);
    }

    public List<SiuInvestigator> getAllSiuInvestigators() {
        return siuInvestigatorRepository.findAll();
    }

    public SiuInvestigator getSiuInvestigatorById(Long id) {
        return siuInvestigatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SIU investigator not found with ID: " + id));
    }

    public SiuInvestigator getByUsername(String username) {
        return siuInvestigatorRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("SIU investigator not found for username: " + username));
    }

    @Transactional
    public void deleteSiuInvestigator(Long id) {
        SiuInvestigator investigator = getSiuInvestigatorById(id);
        log.info("Deleting SIU investigator ID={} username={}", id, investigator.getUser().getUsername());
        siuInvestigatorRepository.delete(investigator);
    }
}

