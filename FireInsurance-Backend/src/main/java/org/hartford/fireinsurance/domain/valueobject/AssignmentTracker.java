package org.hartford.fireinsurance.domain.valueobject;

import jakarta.persistence.*;
import org.hartford.fireinsurance.model.User;

import java.time.LocalDateTime;

/**
 * Value object for tracking claim assignments.
 * Enforces business rule: Only ONE active assignment per claim at any time.
 */
@Embeddable
public class AssignmentTracker {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_assignee_id")
    private User currentAssignee;

    @Column(name = "assignee_type")
    @Enumerated(EnumType.STRING)
    private AssigneeType assigneeType;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "assignment_due_date")
    private LocalDateTime dueDate;

    @Column(name = "assignment_notes")
    private String assignmentNotes;

    // Default constructor for JPA
    public AssignmentTracker() {
    }

    /**
     * Creates a new assignment tracker with no initial assignment.
     */
    public static AssignmentTracker unassigned() {
        return new AssignmentTracker();
    }

    /**
     * Assigns the claim to a user. Enforces single assignment rule.
     *
     * @param assignee the user to assign to
     * @param type the type of assignee
     * @param dueDate when the assignment is due
     * @param notes additional assignment notes
     * @throws IllegalAssignmentException if claim is already assigned
     */
    public void assignTo(User assignee, AssigneeType type, LocalDateTime dueDate, String notes) {
        if (hasActiveAssignment()) {
            throw new IllegalAssignmentException(
                String.format("Claim already assigned to %s (%s). Clear current assignment first.",
                    currentAssignee.getUsername(), assigneeType));
        }

        this.currentAssignee = assignee;
        this.assigneeType = type;
        this.assignedAt = LocalDateTime.now();
        this.dueDate = dueDate;
        this.assignmentNotes = notes;
    }

    /**
     * Reassigns the claim to a different user.
     *
     * @param newAssignee the new user to assign to
     * @param newType the new assignee type
     * @param newDueDate the new due date
     * @param reason reason for reassignment
     */
    public void reassignTo(User newAssignee, AssigneeType newType, LocalDateTime newDueDate, String reason) {
        clearAssignment();
        assignTo(newAssignee, newType, newDueDate, reason);
    }

    /**
     * Clears the current assignment.
     */
    public void clearAssignment() {
        this.currentAssignee = null;
        this.assigneeType = null;
        this.assignedAt = null;
        this.dueDate = null;
        this.assignmentNotes = null;
    }

    /**
     * Checks if there is an active assignment.
     */
    public boolean hasActiveAssignment() {
        return currentAssignee != null && assigneeType != null;
    }

    /**
     * Checks if the assignment is overdue.
     */
    public boolean isOverdue() {
        return hasActiveAssignment() && dueDate != null && LocalDateTime.now().isAfter(dueDate);
    }

    /**
     * Checks if the assignment is assigned to a specific user.
     */
    public boolean isAssignedTo(User user) {
        return hasActiveAssignment() && currentAssignee.equals(user);
    }

    /**
     * Checks if the assignment is of a specific type.
     */
    public boolean isAssignedToType(AssigneeType type) {
        return hasActiveAssignment() && assigneeType == type;
    }

    /**
     * Convenience method: Checks if assigned to an underwriter.
     */
    public boolean isAssignedToUnderwriter() {
        return isAssignedToType(AssigneeType.UNDERWRITER);
    }

    /**
     * Convenience method: Checks if assigned to a SIU investigator.
     */
    public boolean isAssignedToSiuInvestigator() {
        return isAssignedToType(AssigneeType.SIU_INVESTIGATOR);
    }

    /**
     * Convenience method: Checks if assigned to a surveyor.
     */
    public boolean isAssignedToSurveyor() {
        return isAssignedToType(AssigneeType.SURVEYOR);
    }

    // Getters
    public User getCurrentAssignee() {
        return currentAssignee;
    }

    public AssigneeType getAssigneeType() {
        return assigneeType;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public String getAssignmentNotes() {
        return assignmentNotes;
    }

    @Override
    public String toString() {
        if (!hasActiveAssignment()) {
            return "AssignmentTracker{unassigned}";
        }
        return String.format("AssignmentTracker{assignee=%s, type=%s, assigned=%s}",
            currentAssignee.getUsername(), assigneeType, assignedAt);
    }

    /**
     * Exception thrown when attempting to create an invalid assignment.
     */
    public static class IllegalAssignmentException extends RuntimeException {
        public IllegalAssignmentException(String message) {
            super(message);
        }
    }

    /**
     * Enum defining types of assignees in the fraud detection workflow.
     */
    public enum AssigneeType {
        UNDERWRITER("Underwriter"),
        SURVEYOR("Surveyor"),
        SIU_INVESTIGATOR("SIU Investigator"),
        ADMIN("Administrator");

        private final String displayName;

        AssigneeType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}