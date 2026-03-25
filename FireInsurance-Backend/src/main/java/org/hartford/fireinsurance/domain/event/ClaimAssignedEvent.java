package org.hartford.fireinsurance.domain.event;

import java.time.LocalDateTime;

/**
 * Domain event published when a claim is assigned to a user.
 * This event enables tracking of assignment history and notifications.
 */
public class ClaimAssignedEvent extends DomainEvent {

    private final String assigneeUsername;
    private final String assigneeType;
    private final String assignedBy;
    private final LocalDateTime dueDate;
    private final String notes;

    public ClaimAssignedEvent(Long claimId, String assigneeUsername, String assigneeType,
                            String assignedBy, LocalDateTime dueDate, String notes) {
        super(claimId);
        this.assigneeUsername = assigneeUsername;
        this.assigneeType = assigneeType;
        this.assignedBy = assignedBy;
        this.dueDate = dueDate;
        this.notes = notes;
    }

    public Long getClaimId() {
        return getAggregateId();
    }

    public String getAssigneeUsername() {
        return assigneeUsername;
    }

    public String getAssigneeType() {
        return assigneeType;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public String getEventDetails() {
        return String.format("Claim %d assigned to %s (%s) by %s",
            getClaimId(), assigneeUsername, assigneeType, assignedBy);
    }

    @Override
    public String toString() {
        return String.format("ClaimAssigned{claimId=%d, assignee=%s (%s), by=%s}",
                           getClaimId(), assigneeUsername, assigneeType, assignedBy);
    }
}