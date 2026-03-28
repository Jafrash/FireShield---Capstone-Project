package org.hartford.fireinsurance.service;
import org.hartford.fireinsurance.dto.ClaimInspectionRequest;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.ClaimInspection;
import org.hartford.fireinsurance.model.ClaimInspection.ClaimInspectionStatus;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.repository.ClaimInspectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Service
@Transactional
public class ClaimInspectionService {
    private final ClaimInspectionRepository claimInspectionRepository;
    private final ClaimService claimService;
    private final SurveyorService surveyorService;
    public ClaimInspectionService(ClaimInspectionRepository claimInspectionRepository,
                                 ClaimService claimService,
                                 SurveyorService surveyorService) {
        this.claimInspectionRepository = claimInspectionRepository;
        this.claimService = claimService;
        this.surveyorService = surveyorService;
    }
    public ClaimInspection assignSurveyor(Long claimId, Long surveyorId) {
        Claim claim = claimService.getClaimById(claimId);
        Surveyor surveyor = surveyorService.getSurveyor(surveyorId);
        ClaimInspection inspection = new ClaimInspection();
        inspection.setClaim(claim);
        inspection.setSurveyor(surveyor);
        inspection.setStatus(ClaimInspectionStatus.ASSIGNED);

        claimService.updateClaimStatus(claimId, Claim.ClaimStatus.SURVEYOR_ASSIGNED);
        return claimInspectionRepository.save(inspection);
    }

    public ClaimInspection submitInspection(Long inspectionId, String username, ClaimInspectionRequest request) {
        ClaimInspection inspection = claimInspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("Claim Inspection not found"));
        if (!inspection.getSurveyor().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized: Inspection does not belong to this surveyor");
        }
        inspection.setEstimatedLoss(request.getEstimatedLoss());
        inspection.setDamageReport(request.getDamageReport());
        inspection.setInspectionDate(LocalDateTime.now());
        inspection.setStatus(ClaimInspectionStatus.UNDER_REVIEW);

        if (request.getCauseOfFire() != null) {
            inspection.setCauseOfFire(request.getCauseOfFire());
        }
        if (request.getSalvageValue() != null) {
            inspection.setSalvageValue(request.getSalvageValue());
        }
        if (request.getInspectionReport() != null) {
            inspection.setInspectionReport(request.getInspectionReport());
        }
        if (request.getSettlementAmount() != null) {
            inspection.setSettlementAmount(request.getSettlementAmount());
        }
        if (request.getFireBrigadeExpenses() != null) {
            inspection.setFireBrigadeExpenses(request.getFireBrigadeExpenses());
        }
        if (request.getOtherInsuranceDetails() != null) {
            inspection.setOtherInsuranceDetails(request.getOtherInsuranceDetails());
        }
        if (request.getUnderInsuranceDetected() != null) {
            inspection.setUnderInsuranceDetected(request.getUnderInsuranceDetected());
        }
        if (request.getRecommendedSettlement() != null) {
            inspection.setRecommendedSettlement(request.getRecommendedSettlement());
        }

        Claim claim = inspection.getClaim();
        claimService.updateClaimStatus(claim.getClaimId(), Claim.ClaimStatus.SURVEY_COMPLETED);

        return claimInspectionRepository.save(inspection);
    }
    public List<ClaimInspection> getBySurveyor(String username) {
        Surveyor surveyor = surveyorService.getSurveyorByUsername(username);
        return claimInspectionRepository.findBySurveyor(surveyor);
    }
    public List<ClaimInspection> getAll() {
        return claimInspectionRepository.findAll();
    }
    public ClaimInspection getById(Long id) {
        return claimInspectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Claim Inspection not found"));
    }
    public ClaimInspection getByClaim(Long claimId) {
        Claim claim = claimService.getClaimById(claimId);
        return claimInspectionRepository.findByClaim(claim)
                .orElseThrow(() -> new RuntimeException("Claim Inspection not found for claim: " + claimId));
    }
}
