package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.FraudAnalysisResponse;
import org.hartford.fireinsurance.service.FraudAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud")
@PreAuthorize("hasRole('ADMIN')")
public class FraudAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(FraudAnalysisController.class);

    private final FraudAnalysisService fraudAnalysisService;

    public FraudAnalysisController(FraudAnalysisService fraudAnalysisService) {
        this.fraudAnalysisService = fraudAnalysisService;
    }

    @GetMapping("/analysis/{claimId}")
    public ResponseEntity<FraudAnalysisResponse> getFraudAnalysis(@PathVariable Long claimId) {
        try {
            log.info("Fraud analysis requested for claim ID: {}", claimId);

            FraudAnalysisResponse analysis = fraudAnalysisService.generateFraudAnalysis(claimId);

            log.info("Fraud analysis completed for claim ID: {} with score: {}%",
                    claimId, analysis.getFraudScore());

            return ResponseEntity.ok(analysis);

        } catch (RuntimeException e) {
            log.error("Error generating fraud analysis for claim {}: {}", claimId, e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Unexpected error generating fraud analysis for claim {}: {}", claimId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}