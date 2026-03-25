package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.model.InvestigationCase;
import org.hartford.fireinsurance.model.InvestigationStatus;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.SiuInvestigator;
import org.hartford.fireinsurance.repository.InvestigationCaseRepository;
import org.hartford.fireinsurance.repository.ClaimRepository;
import org.hartford.fireinsurance.repository.SiuInvestigatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing SIU investigation cases.
 * Handles the complete lifecycle of investigation cases from creation to completion.
 */
@Service
@Transactional
public class InvestigationCaseService {

    private static final Logger log = LoggerFactory.getLogger(InvestigationCaseService.class);

    @Autowired
    private InvestigationCaseRepository investigationCaseRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private SiuInvestigatorRepository siuInvestigatorRepository;

    /**
     * Create a new investigation case for a claim.
     */
    public InvestigationCase createInvestigationCase(Long claimId, Long investigatorId, String initialNotes, String createdBy) {
        log.info("Creating investigation case for claim ID: {} with investigator ID: {}", claimId, investigatorId);

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found with ID: " + claimId));

        SiuInvestigator investigator = siuInvestigatorRepository.findById(investigatorId)
                .orElseThrow(() -> new RuntimeException("SIU Investigator not found with ID: " + investigatorId));

        // Check if investigation case already exists for this claim
        Optional<InvestigationCase> existingCase = investigationCaseRepository.findByClaim(claim);
        if (existingCase.isPresent()) {
            log.warn("Investigation case already exists for claim ID: {}", claimId);
            throw new RuntimeException("Investigation case already exists for this claim");
        }

        // Determine priority level based on fraud score
        Integer priorityLevel = calculatePriorityLevel(claim);

        InvestigationCase investigationCase = new InvestigationCase(claim, investigator, initialNotes);
        investigationCase.setPriorityLevel(priorityLevel);
        investigationCase.setCreatedBy(createdBy);

        // Update claim's fraud status to indicate investigation
        claim.setFraudStatus(Claim.FraudStatus.SIU_INVESTIGATION);
        claim.setSiuInvestigator(investigator);
        claimRepository.save(claim);

        InvestigationCase saved = investigationCaseRepository.save(investigationCase);
        log.info("Successfully created investigation case ID: {} for claim ID: {}", saved.getInvestigationId(), claimId);

        return saved;
    }

    /**
     * Get all investigation cases assigned to a specific investigator.
     */
    public List<InvestigationCase> getCasesByInvestigator(Long investigatorId) {
        log.info("Fetching cases for investigator ID: {}", investigatorId);

        SiuInvestigator investigator = siuInvestigatorRepository.findById(investigatorId)
                .orElseThrow(() -> new RuntimeException("SIU Investigator not found with ID: " + investigatorId));

        return investigationCaseRepository.findByAssignedInvestigatorOrderByPriorityAndDate(investigator);
    }

    /**
     * Get investigation case by ID with full details.
     */
    public InvestigationCase getCaseById(Long investigationId) {
        log.info("Fetching investigation case ID: {}", investigationId);

        return investigationCaseRepository.findById(investigationId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationId));
    }

    /**
     * Update investigation case status.
     */
    public InvestigationCase updateCaseStatus(Long investigationId, InvestigationStatus newStatus, String updatedBy) {
        log.info("Updating investigation case ID: {} to status: {}", investigationId, newStatus);

        InvestigationCase investigationCase = getCaseById(investigationId);
        InvestigationStatus oldStatus = investigationCase.getStatus();

        investigationCase.setStatus(newStatus);

        // Update timestamps based on status changes
        switch (newStatus) {
            case INVESTIGATING:
                if (oldStatus == InvestigationStatus.ASSIGNED) {
                    investigationCase.setStartedDate(LocalDateTime.now());
                }
                break;
            case COMPLETED:
                investigationCase.setCompletedDate(LocalDateTime.now());
                // Update claim fraud status if investigation is completed
                Claim claim = investigationCase.getClaim();
                if (claim.getFraudStatus() == Claim.FraudStatus.SIU_INVESTIGATION) {
                    claim.setFraudStatus(Claim.FraudStatus.CLEARED); // Default to cleared, can be updated later
                    claimRepository.save(claim);
                }
                break;
        }

        InvestigationCase updated = investigationCaseRepository.save(investigationCase);
        log.info("Successfully updated investigation case ID: {} status to: {}", investigationId, newStatus);

        return updated;
    }

    /**
     * Add investigation note to a case.
     */
    public InvestigationCase addInvestigationNote(Long investigationId, String note, String addedBy) {
        log.info("Adding investigation note to case ID: {}", investigationId);

        InvestigationCase investigationCase = getCaseById(investigationId);

        // Append note to existing notes with timestamp
        String timestamp = LocalDateTime.now().toString();
        String newNote = String.format("[%s - %s]: %s", timestamp, addedBy, note);

        String existingNotes = investigationCase.getInitialNotes();
        if (existingNotes == null || existingNotes.trim().isEmpty()) {
            investigationCase.setInitialNotes(newNote);
        } else {
            investigationCase.setInitialNotes(existingNotes + "\n\n" + newNote);
        }

        InvestigationCase updated = investigationCaseRepository.save(investigationCase);
        log.info("Successfully added note to investigation case ID: {}", investigationId);

        return updated;
    }

    /**
     * Get all cases for SIU dashboard (all cases in the system).
     */
    public List<InvestigationCase> getAllSiuCases() {
        log.info("Fetching all SIU investigation cases");
        return investigationCaseRepository.findAll();
    }

    /**
     * Get case statistics by investigator.
     */
    public CaseStatistics getCaseStatistics(Long investigatorId) {
        log.info("Fetching case statistics for investigator ID: {}", investigatorId);

        SiuInvestigator investigator = siuInvestigatorRepository.findById(investigatorId)
                .orElseThrow(() -> new RuntimeException("SIU Investigator not found with ID: " + investigatorId));

        List<InvestigationCase> allCases = investigationCaseRepository.findByAssignedInvestigator(investigator);

        long assignedCount = allCases.stream().mapToLong(c -> c.getStatus() == InvestigationStatus.ASSIGNED ? 1 : 0).sum();
        long investigatingCount = allCases.stream().mapToLong(c -> c.getStatus() == InvestigationStatus.INVESTIGATING ? 1 : 0).sum();
        long underReviewCount = allCases.stream().mapToLong(c -> c.getStatus() == InvestigationStatus.UNDER_REVIEW ? 1 : 0).sum();
        long completedCount = allCases.stream().mapToLong(c -> c.getStatus() == InvestigationStatus.COMPLETED ? 1 : 0).sum();
        long highPriorityCount = allCases.stream().mapToLong(c -> c.getPriorityLevel() != null && c.getPriorityLevel() <= 2 ? 1 : 0).sum();

        return new CaseStatistics(
                allCases.size(),
                (int) assignedCount,
                (int) investigatingCount,
                (int) underReviewCount,
                (int) completedCount,
                (int) highPriorityCount
        );
    }

    /**
     * Calculate priority level based on claim fraud score and other factors.
     */
    private Integer calculatePriorityLevel(Claim claim) {
        Double fraudScore = claim.getFraudScore();
        if (fraudScore == null) {
            return 3; // Medium priority if no fraud score
        }

        if (fraudScore >= 90) {
            return 1; // Critical
        } else if (fraudScore >= 80) {
            return 2; // High
        } else if (fraudScore >= 60) {
            return 3; // Medium
        } else if (fraudScore >= 40) {
            return 4; // Low
        } else {
            return 5; // Lowest
        }
    }

    /**
     * Inner class for case statistics.
     */
    public static class CaseStatistics {
        private final int totalCases;
        private final int assignedCases;
        private final int investigatingCases;
        private final int underReviewCases;
        private final int completedCases;
        private final int highPriorityCases;

        public CaseStatistics(int totalCases, int assignedCases, int investigatingCases,
                            int underReviewCases, int completedCases, int highPriorityCases) {
            this.totalCases = totalCases;
            this.assignedCases = assignedCases;
            this.investigatingCases = investigatingCases;
            this.underReviewCases = underReviewCases;
            this.completedCases = completedCases;
            this.highPriorityCases = highPriorityCases;
        }

        // Getters
        public int getTotalCases() { return totalCases; }
        public int getAssignedCases() { return assignedCases; }
        public int getInvestigatingCases() { return investigatingCases; }
        public int getUnderReviewCases() { return underReviewCases; }
        public int getCompletedCases() { return completedCases; }
        public int getHighPriorityCases() { return highPriorityCases; }
    }
}