package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.model.InvestigationActivity;
import org.hartford.fireinsurance.model.InvestigationCase;
import org.hartford.fireinsurance.repository.InvestigationActivityRepository;
import org.hartford.fireinsurance.repository.InvestigationCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for managing investigation activities and timeline tracking.
 * Provides comprehensive activity logging and timeline management for SIU investigations.
 */
@Service
@Transactional
public class InvestigationActivityService {

    private static final Logger log = LoggerFactory.getLogger(InvestigationActivityService.class);

    @Autowired
    private InvestigationActivityRepository investigationActivityRepository;

    @Autowired
    private InvestigationCaseRepository investigationCaseRepository;

    /**
     * Add a custom activity to an investigation case.
     */
    public InvestigationActivity addActivity(Long investigationCaseId, InvestigationActivity.ActivityType activityType,
                                           String description, String performedBy, String additionalDetails) {
        log.info("Adding activity to investigation case ID: {}, type: {}", investigationCaseId, activityType);

        InvestigationCase investigationCase = investigationCaseRepository.findById(investigationCaseId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationCaseId));

        InvestigationActivity activity = new InvestigationActivity(investigationCase, activityType, description, performedBy);
        activity.setAdditionalDetails(additionalDetails);
        activity.setSystemGenerated(false); // User-generated activity

        InvestigationActivity saved = investigationActivityRepository.save(activity);
        log.info("Successfully added activity ID: {} to investigation case ID: {}", saved.getActivityId(), investigationCaseId);

        return saved;
    }

    /**
     * Add a system-generated activity (used internally by other services).
     */
    public InvestigationActivity addSystemActivity(Long investigationCaseId, InvestigationActivity.ActivityType activityType,
                                                 String description, String performedBy) {
        return addSystemActivity(investigationCaseId, activityType, description, performedBy, null);
    }

    /**
     * Add a system-generated activity with additional details.
     */
    public InvestigationActivity addSystemActivity(Long investigationCaseId, InvestigationActivity.ActivityType activityType,
                                                 String description, String performedBy, String additionalDetails) {
        log.info("Adding system activity to investigation case ID: {}, type: {}", investigationCaseId, activityType);

        InvestigationCase investigationCase = investigationCaseRepository.findById(investigationCaseId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationCaseId));

        InvestigationActivity activity = new InvestigationActivity(investigationCase, activityType, description, performedBy, true);
        activity.setAdditionalDetails(additionalDetails);

        InvestigationActivity saved = investigationActivityRepository.save(activity);
        log.info("Successfully added system activity ID: {} to investigation case ID: {}", saved.getActivityId(), investigationCaseId);

        return saved;
    }

    /**
     * Get complete timeline for an investigation case.
     */
    public List<InvestigationActivity> getTimelineByCase(Long investigationCaseId) {
        log.info("Fetching timeline for investigation case ID: {}", investigationCaseId);

        InvestigationCase investigationCase = investigationCaseRepository.findById(investigationCaseId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationCaseId));

        return investigationActivityRepository.getTimelineByCase(investigationCase);
    }

    /**
     * Get recent activities for an investigation case (last N days).
     */
    public List<InvestigationActivity> getRecentActivities(Long investigationCaseId, int days) {
        log.info("Fetching recent activities for investigation case ID: {} (last {} days)", investigationCaseId, days);

        InvestigationCase investigationCase = investigationCaseRepository.findById(investigationCaseId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationCaseId));

        LocalDateTime sinceDate = LocalDateTime.now().minusDays(days);
        return investigationActivityRepository.findRecentActivitiesByCase(investigationCase, sinceDate);
    }

    /**
     * Get activities by type for an investigation case.
     */
    public List<InvestigationActivity> getActivitiesByType(Long investigationCaseId, InvestigationActivity.ActivityType activityType) {
        log.info("Fetching activities by type {} for investigation case ID: {}", activityType, investigationCaseId);

        InvestigationCase investigationCase = investigationCaseRepository.findById(investigationCaseId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationCaseId));

        return investigationActivityRepository.findByInvestigationCaseAndActivityType(investigationCase, activityType);
    }

    /**
     * Search activities by description.
     */
    public List<InvestigationActivity> searchActivities(Long investigationCaseId, String searchTerm) {
        log.info("Searching activities for investigation case ID: {} with term: {}", investigationCaseId, searchTerm);

        InvestigationCase investigationCase = investigationCaseRepository.findById(investigationCaseId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationCaseId));

        return investigationActivityRepository.findByInvestigationCaseAndDescriptionContaining(investigationCase, searchTerm);
    }

    /**
     * Get activity statistics for an investigation case.
     */
    public ActivityStatistics getActivityStatistics(Long investigationCaseId) {
        log.info("Fetching activity statistics for investigation case ID: {}", investigationCaseId);

        InvestigationCase investigationCase = investigationCaseRepository.findById(investigationCaseId)
                .orElseThrow(() -> new RuntimeException("Investigation case not found with ID: " + investigationCaseId));

        List<InvestigationActivity> allActivities = investigationActivityRepository.findByInvestigationCase(investigationCase);
        List<Object[]> statisticsByType = investigationActivityRepository.getActivityStatisticsByCase(investigationCase);

        long totalActivities = allActivities.size();
        long systemGenerated = allActivities.stream().mapToLong(a -> Boolean.TRUE.equals(a.getSystemGenerated()) ? 1 : 0).sum();
        long userGenerated = totalActivities - systemGenerated;

        // Convert statistics by type to a map
        Map<InvestigationActivity.ActivityType, Long> activityTypeCounts = statisticsByType.stream()
                .collect(Collectors.toMap(
                    row -> (InvestigationActivity.ActivityType) row[0],
                    row -> (Long) row[1]
                ));

        // Calculate investigation duration
        LocalDateTime firstActivity = allActivities.stream()
                .map(InvestigationActivity::getPerformedAt)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime lastActivity = allActivities.stream()
                .map(InvestigationActivity::getPerformedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        long investigationDurationHours = ChronoUnit.HOURS.between(firstActivity, lastActivity);

        return new ActivityStatistics(totalActivities, systemGenerated, userGenerated,
                                    activityTypeCounts, investigationDurationHours, firstActivity, lastActivity);
    }

    /**
     * Bulk add activities (useful for data migration or batch operations).
     */
    public List<InvestigationActivity> addBulkActivities(List<InvestigationActivity> activities) {
        log.info("Adding {} activities in bulk", activities.size());

        List<InvestigationActivity> saved = investigationActivityRepository.saveAll(activities);
        log.info("Successfully saved {} activities in bulk", saved.size());

        return saved;
    }

    /**
     * Delete activity (should be used carefully as it affects audit trail).
     */
    public void deleteActivity(Long activityId, String deletedBy) {
        log.warn("Deleting activity ID: {} by user: {} (THIS AFFECTS AUDIT TRAIL)", activityId, deletedBy);

        InvestigationActivity activity = investigationActivityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found with ID: " + activityId));

        // Log the deletion as a new activity before deleting
        InvestigationActivity deletionRecord = new InvestigationActivity(
                activity.getInvestigationCase(),
                InvestigationActivity.ActivityType.CUSTOM,
                "Activity deleted: " + activity.getActivityDescription(),
                deletedBy,
                true
        );
        investigationActivityRepository.save(deletionRecord);

        // Delete the activity
        investigationActivityRepository.delete(activity);
        log.info("Successfully deleted activity ID: {}", activityId);
    }

    // Helper methods for common activity types
    public InvestigationActivity logInterviewActivity(Long investigationCaseId, String intervieweeName, String conductedBy) {
        String description = String.format("Interview conducted with %s", intervieweeName);
        return addActivity(investigationCaseId, InvestigationActivity.ActivityType.INTERVIEW_CONDUCTED, description, conductedBy, null);
    }

    public InvestigationActivity logSiteVisitActivity(Long investigationCaseId, String location, String conductedBy) {
        String description = String.format("Site visit conducted at %s", location);
        return addActivity(investigationCaseId, InvestigationActivity.ActivityType.SITE_VISIT, description, conductedBy, null);
    }

    public InvestigationActivity logDocumentReviewActivity(Long investigationCaseId, String documentName, String reviewedBy) {
        String description = String.format("Document reviewed: %s", documentName);
        return addActivity(investigationCaseId, InvestigationActivity.ActivityType.DOCUMENT_REVIEWED, description, reviewedBy, null);
    }

    /**
     * Inner class for activity statistics.
     */
    public static class ActivityStatistics {
        private final long totalActivities;
        private final long systemGenerated;
        private final long userGenerated;
        private final Map<InvestigationActivity.ActivityType, Long> activityTypeCounts;
        private final long investigationDurationHours;
        private final LocalDateTime firstActivity;
        private final LocalDateTime lastActivity;

        public ActivityStatistics(long totalActivities, long systemGenerated, long userGenerated,
                                Map<InvestigationActivity.ActivityType, Long> activityTypeCounts,
                                long investigationDurationHours, LocalDateTime firstActivity, LocalDateTime lastActivity) {
            this.totalActivities = totalActivities;
            this.systemGenerated = systemGenerated;
            this.userGenerated = userGenerated;
            this.activityTypeCounts = activityTypeCounts;
            this.investigationDurationHours = investigationDurationHours;
            this.firstActivity = firstActivity;
            this.lastActivity = lastActivity;
        }

        // Getters
        public long getTotalActivities() { return totalActivities; }
        public long getSystemGenerated() { return systemGenerated; }
        public long getUserGenerated() { return userGenerated; }
        public Map<InvestigationActivity.ActivityType, Long> getActivityTypeCounts() { return activityTypeCounts; }
        public long getInvestigationDurationHours() { return investigationDurationHours; }
        public LocalDateTime getFirstActivity() { return firstActivity; }
        public LocalDateTime getLastActivity() { return lastActivity; }

        public String getFormattedDuration() {
            if (investigationDurationHours < 24) {
                return investigationDurationHours + " hours";
            } else {
                long days = investigationDurationHours / 24;
                long hours = investigationDurationHours % 24;
                return days + " days, " + hours + " hours";
            }
        }
    }
}