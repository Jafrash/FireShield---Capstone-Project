package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.AssignUnderwriterRequest;
import org.hartford.fireinsurance.dto.AssignSiuRequest;
import org.hartford.fireinsurance.dto.BlacklistRequest;
import org.hartford.fireinsurance.dto.BlacklistResponse;
import org.hartford.fireinsurance.dto.SurveyorRegistrationRequest;
import org.hartford.fireinsurance.dto.SurveyorResponse;
import org.hartford.fireinsurance.dto.SubscriptionResponse;
import org.hartford.fireinsurance.dto.UnderwriterRegistrationRequest;
import org.hartford.fireinsurance.dto.UnderwriterResponse;
import org.hartford.fireinsurance.dto.SiuInvestigatorRegistrationRequest;
import org.hartford.fireinsurance.dto.SiuInvestigatorResponse;
import org.hartford.fireinsurance.dto.FraudStatisticsResponse;
import org.hartford.fireinsurance.dto.FraudDistributionResponse;
import org.hartford.fireinsurance.dto.SiuWorkloadResponse;
import org.hartford.fireinsurance.dto.FraudTrendResponse;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.model.Underwriter;
import org.hartford.fireinsurance.model.SiuInvestigator;
import org.hartford.fireinsurance.model.BlacklistedUser;
import org.hartford.fireinsurance.service.ClaimService;
import org.hartford.fireinsurance.service.PolicySubscriptionService;
import org.hartford.fireinsurance.service.SurveyorService;
import org.hartford.fireinsurance.service.UnderwriterService;
import org.hartford.fireinsurance.service.SiuService;
import org.hartford.fireinsurance.service.BlacklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UnderwriterService underwriterService;
    private final SurveyorService surveyorService;
    private final PolicySubscriptionService subscriptionService;
    private final ClaimService claimService;
    private final SiuService siuService;
    private final BlacklistService blacklistService;

    public AdminController(UnderwriterService underwriterService,
                           SurveyorService surveyorService,
                           PolicySubscriptionService subscriptionService,
                           ClaimService claimService,
                           SiuService siuService,
                           BlacklistService blacklistService) {
        this.underwriterService = underwriterService;
        this.surveyorService = surveyorService;
        this.subscriptionService = subscriptionService;
        this.claimService = claimService;
        this.siuService = siuService;
        this.blacklistService = blacklistService;
    }

    @PostMapping("/underwriters")
    public ResponseEntity<UnderwriterResponse> createUnderwriter(@RequestBody UnderwriterRegistrationRequest request) {
        Underwriter created = underwriterService.createUnderwriter(request);
        return ResponseEntity.ok(toUnderwriterResponse(created));
    }

    @GetMapping("/underwriters")
    public ResponseEntity<List<UnderwriterResponse>> getUnderwriters() {
        List<UnderwriterResponse> response = underwriterService.getAllUnderwriters().stream()
                .map(this::toUnderwriterResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/surveyors")
    public ResponseEntity<SurveyorResponse> createSurveyor(@RequestBody SurveyorRegistrationRequest request) {
        surveyorService.registerSurveyor(request);
        Surveyor created = surveyorService.getSurveyorByUsername(request.getUsername());
        return ResponseEntity.ok(toSurveyorResponse(created));
    }

    @GetMapping("/surveyors")
    public ResponseEntity<List<SurveyorResponse>> getSurveyors() {
        List<SurveyorResponse> response = surveyorService.getAllSurveyors().stream()
                .map(this::toSurveyorResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    // === SIU Investigator Management ===

    @PostMapping("/siu-investigators")
    public ResponseEntity<SiuInvestigatorResponse> createSiuInvestigator(@RequestBody SiuInvestigatorRegistrationRequest request) {
        SiuInvestigator created = siuService.createSiuInvestigator(request);
        return ResponseEntity.ok(toSiuInvestigatorResponse(created));
    }

    @GetMapping("/siu-investigators")
    public ResponseEntity<List<SiuInvestigatorResponse>> getSiuInvestigators() {
        List<SiuInvestigatorResponse> response = siuService.getAllSiuInvestigators().stream()
                .map(this::toSiuInvestigatorResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    // Alias endpoint for SIU users (matching frontend requirement)
    @GetMapping("/siu-users")
    public ResponseEntity<List<SiuInvestigatorResponse>> getSiuUsers() {
        return getSiuInvestigators(); // Delegate to existing implementation
    }

    @GetMapping("/siu-investigators/{id}")
    public ResponseEntity<SiuInvestigatorResponse> getSiuInvestigatorById(@PathVariable Long id) {
        SiuInvestigator investigator = siuService.getSiuInvestigatorById(id);
        return ResponseEntity.ok(toSiuInvestigatorResponse(investigator));
    }

    @DeleteMapping("/siu-investigators/{id}")
    public ResponseEntity<String> deleteSiuInvestigator(@PathVariable Long id) {
        siuService.deleteSiuInvestigator(id);
        return ResponseEntity.ok("SIU investigator deleted successfully");
    }

    // === Blacklist Management ===

    @PostMapping("/blacklist")
    public ResponseEntity<BlacklistResponse> addToBlacklist(@RequestBody BlacklistRequest request,
                                                           Authentication authentication) {
        BlacklistedUser created = blacklistService.addToBlacklist(request, authentication.getName());
        return ResponseEntity.ok(toBlacklistResponse(created));
    }

    @GetMapping("/blacklist")
    public ResponseEntity<List<BlacklistResponse>> getBlacklistedUsers() {
        List<BlacklistResponse> response = blacklistService.getAllBlacklistedUsers().stream()
                .map(this::toBlacklistResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/blacklist/{id}")
    public ResponseEntity<BlacklistResponse> getBlacklistedUserById(@PathVariable Long id) {
        BlacklistedUser blacklistedUser = blacklistService.getBlacklistedUserById(id);
        return ResponseEntity.ok(toBlacklistResponse(blacklistedUser));
    }

    @DeleteMapping("/blacklist/{id}")
    public ResponseEntity<String> removeFromBlacklist(@PathVariable Long id, Authentication authentication) {
        blacklistService.removeFromBlacklist(id, authentication.getName());
        return ResponseEntity.ok("User removed from blacklist successfully");
    }

    @GetMapping("/blacklist/check/{identifier}")
    public ResponseEntity<Boolean> checkBlacklist(@PathVariable String identifier) {
        boolean isBlacklisted = blacklistService.isUserBlacklisted(identifier);
        return ResponseEntity.ok(isBlacklisted);
    }

    @PostMapping("/assign-underwriter/subscription")
    public ResponseEntity<SubscriptionResponse> assignUnderwriterToSubscription(@RequestBody AssignUnderwriterRequest request) {
        PolicySubscription updated = subscriptionService.assignUnderwriter(request.getTargetId(), request.getUnderwriterId());
        return ResponseEntity.ok(new SubscriptionResponse(
                updated.getSubscriptionId(),
                updated.getProperty() != null ? updated.getProperty().getPropertyId() : null,
                updated.getPolicy() != null ? updated.getPolicy().getPolicyName() : null,
                updated.getStartDate(),
                updated.getEndDate(),
                updated.getStatus(),
                updated.getPremiumAmount(),
                updated.getBasePremiumAmount(),
                updated.getRiskScore(),
                updated.getRiskMultiplier(),
                updated.getPropertyInspection() != null ? updated.getPropertyInspection().getInspectionId() : null,
                updated.getUnderwriter() != null ? updated.getUnderwriter().getUnderwriterId() : null,
                updated.getRequestedCoverage()
        ));
    }

    @PostMapping("/assign-underwriter/claim")
    public ResponseEntity<String> assignUnderwriterToClaim(@RequestBody AssignUnderwriterRequest request) {
        try {
            Claim updated = claimService.assignUnderwriter(request.getTargetId(), request.getUnderwriterId());
            return ResponseEntity.ok("Underwriter assigned to claim " + updated.getClaimId());
        } catch (IllegalStateException e) {
            // Add hint for SIU_CLEARED enforcement
            String msg = e.getMessage();
            if (msg != null && msg.contains("SIU_CLEARED")) {
                msg += " Only claims that have been cleared by SIU can be assigned to an underwriter.";
            }
            return ResponseEntity.badRequest().body(msg);
        }
    }

    @PostMapping("/assign-siu")
    public ResponseEntity<String> assignSiuToClaim(@RequestBody AssignSiuRequest request) {
        try {
            Claim updated = claimService.assignSiuInvestigator(request.getClaimId(), request.getInvestigatorId());
            return ResponseEntity.ok("SIU investigator assigned to claim " + updated.getClaimId());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // === Fraud Monitoring Dashboard ===

    @GetMapping("/fraud/statistics")
    public ResponseEntity<FraudStatisticsResponse> getFraudStatistics() {
        try {
            List<Claim> claims = claimService.getAllClaims();
            long totalClaims = claims.size();
            long totalFraudCases = claims.stream().filter(c -> c.getFraudScore() != null && c.getFraudScore() > 0).count();
            long highRiskClaims = claims.stream().filter(c -> org.hartford.fireinsurance.model.RiskLevel.HIGH.equals(c.getRiskLevel())).count();
            long mediumRiskClaims = claims.stream().filter(c -> org.hartford.fireinsurance.model.RiskLevel.MEDIUM.equals(c.getRiskLevel())).count();
            long lowRiskClaims = claims.stream().filter(c -> org.hartford.fireinsurance.model.RiskLevel.LOW.equals(c.getRiskLevel()) && c.getFraudScore() != null && c.getFraudScore() > 0).count();
            
            long totalSiuInvestigations = claims.stream().filter(c -> c.getSiuInvestigator() != null).count();
            long activeSiuInvestigations = claims.stream().filter(c -> "UNDER_INVESTIGATION".equals(c.getSiuStatus())).count();
            long completedSiuInvestigations = claims.stream().filter(c -> "COMPLETED".equals(c.getSiuStatus()) || "SIU_CLEARED".equals(c.getSiuStatus()) || "FRAUD_CONFIRMED".equals(c.getSiuStatus())).count();
            long fraudConfirmedCases = claims.stream().filter(c -> "FRAUD_CONFIRMED".equals(c.getSiuStatus())).count();
            long clearedCases = claims.stream().filter(c -> "SIU_CLEARED".equals(c.getSiuStatus())).count();
            
            double averageFraudScore = claims.stream().mapToDouble(c -> c.getFraudScore() != null ? c.getFraudScore() : 0.0).average().orElse(0.0);
            double totalClaimsValue = claims.stream().mapToDouble(c -> c.getClaimAmount() != null ? c.getClaimAmount() : 0.0).sum();
            double fraudulentClaimsValue = claims.stream()
                    .filter(c -> org.hartford.fireinsurance.model.RiskLevel.HIGH.equals(c.getRiskLevel()))
                    .mapToDouble(c -> c.getClaimAmount() != null ? c.getClaimAmount() : 0.0)
                    .sum();
            double fraudPercentage = totalClaims > 0 ? (double) totalFraudCases / totalClaims * 100 : 0.0;
            
            FraudStatisticsResponse stats = new FraudStatisticsResponse(
                totalFraudCases, highRiskClaims, mediumRiskClaims, lowRiskClaims,
                totalSiuInvestigations, activeSiuInvestigations, completedSiuInvestigations,
                fraudConfirmedCases, clearedCases, averageFraudScore,
                totalClaimsValue, fraudulentClaimsValue, fraudPercentage
            );
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/fraud/distribution")
    public ResponseEntity<List<FraudDistributionResponse>> getFraudDistribution() {
        try {
            List<Claim> claims = claimService.getAllClaims();
            long totalFraudCases = claims.stream().filter(c -> c.getFraudScore() != null && c.getFraudScore() > 0).count();
            
            long highCount = claims.stream().filter(c -> org.hartford.fireinsurance.model.RiskLevel.HIGH.equals(c.getRiskLevel())).count();
            double highValue = claims.stream().filter(c -> org.hartford.fireinsurance.model.RiskLevel.HIGH.equals(c.getRiskLevel())).mapToDouble(c -> c.getClaimAmount() != null ? c.getClaimAmount() : 0.0).sum();
            
            long medCount = claims.stream().filter(c -> org.hartford.fireinsurance.model.RiskLevel.MEDIUM.equals(c.getRiskLevel())).count();
            double medValue = claims.stream().filter(c -> org.hartford.fireinsurance.model.RiskLevel.MEDIUM.equals(c.getRiskLevel())).mapToDouble(c -> c.getClaimAmount() != null ? c.getClaimAmount() : 0.0).sum();
            
            long lowCount = claims.stream().filter(c -> org.hartford.fireinsurance.model.RiskLevel.LOW.equals(c.getRiskLevel()) && c.getFraudScore() != null && c.getFraudScore() > 0).count();
            double lowValue = claims.stream().filter(c -> org.hartford.fireinsurance.model.RiskLevel.LOW.equals(c.getRiskLevel()) && c.getFraudScore() != null && c.getFraudScore() > 0).mapToDouble(c -> c.getClaimAmount() != null ? c.getClaimAmount() : 0.0).sum();
            
            List<FraudDistributionResponse> distribution = java.util.Arrays.asList(
                new FraudDistributionResponse("HIGH", highCount, totalFraudCases > 0 ? (double) highCount / totalFraudCases * 100 : 0.0, highValue),
                new FraudDistributionResponse("MEDIUM", medCount, totalFraudCases > 0 ? (double) medCount / totalFraudCases * 100 : 0.0, medValue),
                new FraudDistributionResponse("LOW", lowCount, totalFraudCases > 0 ? (double) lowCount / totalFraudCases * 100 : 0.0, lowValue)
            );
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/fraud/siu-workload")
    public ResponseEntity<List<SiuWorkloadResponse>> getSiuWorkload() {
        try {
            List<SiuInvestigator> investigators = siuService.getAllSiuInvestigators();
            List<Claim> allClaims = claimService.getAllClaims();
            
            List<SiuWorkloadResponse> workload = investigators.stream().map(inv -> {
                Long active = allClaims.stream().filter(c -> inv.equals(c.getSiuInvestigator()) && "UNDER_INVESTIGATION".equals(c.getSiuStatus())).count();
                Long completed = allClaims.stream().filter(c -> inv.equals(c.getSiuInvestigator()) && ("COMPLETED".equals(c.getSiuStatus()) || "SIU_CLEARED".equals(c.getSiuStatus()) || "FRAUD_CONFIRMED".equals(c.getSiuStatus()))).count();
                Long confirmed = allClaims.stream().filter(c -> inv.equals(c.getSiuInvestigator()) && "FRAUD_CONFIRMED".equals(c.getSiuStatus())).count();
                Long cleared = allClaims.stream().filter(c -> inv.equals(c.getSiuInvestigator()) && "SIU_CLEARED".equals(c.getSiuStatus())).count();
                Double avgDays = Double.valueOf(15.0); // Mocking average days for now as it's not tracked
                return new SiuWorkloadResponse(
                    inv.getInvestigatorId(),
                    inv.getUser().getUsername(),
                    active,
                    completed,
                    confirmed,
                    cleared,
                    avgDays
                );
            }).toList();
            
            return ResponseEntity.ok(workload);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/fraud/trends")
    public ResponseEntity<List<FraudTrendResponse>> getFraudTrends() {
        try {
            // Re-use mock trends for now as temporal aggregation is complex without a dedicated analytics service,
            // but ensure it's not totally static if no data exists.
            List<Claim> claims = claimService.getAllClaims();
            if (claims.isEmpty()) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            return ResponseEntity.ok(generateMockFraudTrends());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper methods

    // === Fraud monitoring helper methods ===

    private FraudStatisticsResponse generateMockFraudStatistics() {
        return new FraudStatisticsResponse(
            47L,        // totalFraudCases
            23L,        // highRiskClaims
            18L,        // mediumRiskClaims
            6L,         // lowRiskClaims
            35L,        // totalSiuInvestigations
            12L,        // activeSiuInvestigations
            23L,        // completedSiuInvestigations
            15L,        // fraudConfirmedCases
            20L,        // clearedCases
            42.3,       // averageFraudScore
            12500000.0, // totalClaimsValue
            3750000.0,  // fraudulentClaimsValue
            12.5        // fraudPercentage
        );
    }

    private List<FraudDistributionResponse> generateMockFraudDistribution() {
        return List.of(
            new FraudDistributionResponse("HIGH", 23L, 48.9, 2800000.0),
            new FraudDistributionResponse("MEDIUM", 18L, 38.3, 850000.0),
            new FraudDistributionResponse("LOW", 6L, 12.8, 100000.0)
        );
    }

    private List<SiuWorkloadResponse> generateMockSiuWorkload() {
        return List.of(
            new SiuWorkloadResponse(1L, "John Smith", 4L, 12L, 7L, 5L, 15.4),
            new SiuWorkloadResponse(2L, "Sarah Johnson", 6L, 18L, 10L, 8L, 12.2),
            new SiuWorkloadResponse(3L, "Mike Davis", 2L, 8L, 3L, 5L, 18.5)
        );
    }

    private List<FraudTrendResponse> generateMockFraudTrends() {
        return List.of(
            new FraudTrendResponse("Nov 2025", 8L, 67L, 11.9),
            new FraudTrendResponse("Dec 2025", 12L, 78L, 15.4),
            new FraudTrendResponse("Jan 2026", 9L, 72L, 12.5),
            new FraudTrendResponse("Feb 2026", 7L, 58L, 12.1),
            new FraudTrendResponse("Mar 2026", 11L, 84L, 13.1),
            new FraudTrendResponse("Apr 2026", 10L, 76L, 13.2)
        );
    }

    // === Model conversion helper methods ===

    private UnderwriterResponse toUnderwriterResponse(Underwriter u) {
        return new UnderwriterResponse(
                u.getUnderwriterId(),
                u.getUser().getUsername(),
                u.getUser().getEmail(),
                u.getUser().getPhoneNumber(),
                u.getDepartment(),
                u.getRegion(),
                u.getExperienceYears(),
                u.getActive(),
                u.getCreatedAt()
        );
    }

    private SurveyorResponse toSurveyorResponse(Surveyor surveyor) {
        return new SurveyorResponse(
                surveyor.getSurveyorId(),
                surveyor.getUser().getUsername(),
                surveyor.getUser().getEmail(),
                surveyor.getUser().getPhoneNumber(),
                surveyor.getLicenseNumber(),
                surveyor.getExperienceYears(),
                surveyor.getAssignedRegion()
        );
    }

    private SiuInvestigatorResponse toSiuInvestigatorResponse(SiuInvestigator investigator) {
        return new SiuInvestigatorResponse(
                investigator.getInvestigatorId(),
                investigator.getUser().getUsername(),
                investigator.getUser().getEmail(),
                investigator.getFirstName(),
                investigator.getLastName(),
                investigator.getUser().getPhoneNumber(),
                investigator.getBadgeNumber(),
                investigator.getDepartment(),
                investigator.getExperienceYears(),
                investigator.getSpecialization(),
                investigator.getActive(),
                investigator.getCreatedAt()
        );
    }

    private BlacklistResponse toBlacklistResponse(BlacklistedUser blacklistedUser) {
        return new BlacklistResponse(
                blacklistedUser.getBlacklistId(),
                blacklistedUser.getUsername(),
                blacklistedUser.getEmail(),
                blacklistedUser.getPhone(),
                blacklistedUser.getReason(),
                blacklistedUser.getActive(),
                blacklistedUser.getCreatedAt(),
                blacklistedUser.getCreatedBy()
        );
    }
}

