package org.hartford.fireinsurance.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final String fromAddress;
    private final boolean debugLogging;

    public EmailNotificationService(
            JavaMailSender mailSender,
            @Value("${app.email.enabled:true}") boolean emailEnabled,
            @Value("${app.email.from:noreply@fireshield.local}") String fromAddress,
            @Value("${app.email.enable-debug-logging:false}") boolean debugLogging
    ) {
        this.mailSender = mailSender;
        this.emailEnabled = emailEnabled;
        this.fromAddress = fromAddress;
        this.debugLogging = debugLogging;
    }

    /**
     * Send email notification with event-based templating.
     * Non-blocking: logs errors but does not throw exceptions.
     */
    public void sendEmailNotification(String toEmail, String eventKey, Map<String, String> templateVars) {
        if (!emailEnabled) {
            if (debugLogging) {
                log.debug("Email notifications are disabled globally. Skipping email to {}", toEmail);
            }
            return;
        }

        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Cannot send email notification: recipient email is null or blank");
            return;
        }

        try {
            String subject = buildSubject(eventKey, templateVars);
            String body = buildBody(eventKey, templateVars);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            if (debugLogging) {
                log.debug("Email sent successfully to {} for event: {}", toEmail, eventKey);
            }
        } catch (Exception e) {
            // Non-blocking: log but don't throw. This ensures business logic continues.
            log.error("Failed to send email notification to {} for event {}: {}", toEmail, eventKey, e.getMessage(), e);
        }
    }

    /**
     * Send bulk email notifications (e.g., renewal reminders).
     * Non-blocking for all failures.
     */
    public void sendBulkEmailNotification(String[] toEmails, String eventKey, Map<String, String> templateVars) {
        if (toEmails == null || toEmails.length == 0) {
            log.warn("Bulk email notification called with empty recipient list");
            return;
        }

        for (String email : toEmails) {
            sendEmailNotification(email, eventKey, templateVars);
        }
    }

    /**
     * Build email subject based on event key and template variables.
     */
    private String buildSubject(String eventKey, Map<String, String> vars) {
        return switch (eventKey) {
            case "POLICY_SUBMITTED" -> "FireShield: Policy Application Received";
            case "CLAIM_STATUS_CHANGE" -> "FireShield: Claim #" + vars.getOrDefault("claimId", "N/A") + " Status Update";
            case "INSPECTION_ASSIGNED" -> "FireShield: New Inspection Assignment";
            case "INSPECTION_COMPLETED" -> "FireShield: Inspection Completed - " + vars.getOrDefault("inspectionId", "N/A");
            case "POLICY_APPROVAL" -> "FireShield: Policy Approved";
            case "POLICY_REJECTION" -> "FireShield: Policy Update";
            case "POLICY_RENEWAL_REMINDER" -> "FireShield: Policy Renewal Reminder";
            default -> "FireShield Notification";
        };
    }

    /**
     * Build email body based on event key and template variables.
     */
    private String buildBody(String eventKey, Map<String, String> vars) {
        return switch (eventKey) {
            case "POLICY_SUBMITTED" -> buildPolicySubmittedBody(vars);
            case "CLAIM_STATUS_CHANGE" -> buildClaimStatusBody(vars);
            case "INSPECTION_ASSIGNED" -> buildInspectionAssignedBody(vars);
            case "INSPECTION_COMPLETED" -> buildInspectionCompletedBody(vars);
            case "POLICY_APPROVAL" -> buildPolicyApprovalBody(vars);
            case "POLICY_REJECTION" -> buildPolicyRejectionBody(vars);
            case "POLICY_RENEWAL_REMINDER" -> buildRenewalReminderBody(vars);
            default -> buildDefaultBody(vars);
        };
    }

    private String buildClaimStatusBody(Map<String, String> vars) {
        return String.format(
                "Hello %s,\n\n" +
                "Your claim #%s has been updated.\n" +
                "Current Status: %s\n" +
                "Details: %s\n\n" +
                "Please log in to your FireShield dashboard for more information.\n\n" +
                "Best regards,\nFireShield Insurance Team",
                vars.getOrDefault("customerName", "Valued Customer"),
                vars.getOrDefault("claimId", "N/A"),
                vars.getOrDefault("status", "UNKNOWN"),
                vars.getOrDefault("details", "No additional details provided")
        );
    }

    private String buildInspectionAssignedBody(Map<String, String> vars) {
        return String.format(
                "Hello %s,\n\n" +
                "You have been assigned to inspection #%s.\n" +
                "Property: %s\n" +
                "Scheduled Date: %s\n\n" +
                "Please log in to your FireShield dashboard to view details and accept the assignment.\n\n" +
                "Best regards,\nFireShield Insurance Team",
                vars.getOrDefault("surveyorName", "Valued Surveyor"),
                vars.getOrDefault("inspectionId", "N/A"),
                vars.getOrDefault("propertyAddress", "Address not provided"),
                vars.getOrDefault("scheduledDate", "TBD")
        );
    }

    private String buildInspectionCompletedBody(Map<String, String> vars) {
        return String.format(
                "Hello %s,\n\n" +
                "Inspection #%s has been completed successfully.\n" +
                "Inspector: %s\n" +
                "Completion Date: %s\n\n" +
                "The report is now available in your FireShield dashboard.\n\n" +
                "Best regards,\nFireShield Insurance Team",
                vars.getOrDefault("customerName", "Valued Customer"),
                vars.getOrDefault("inspectionId", "N/A"),
                vars.getOrDefault("surveyorName", "Inspector"),
                vars.getOrDefault("completionDate", "Today")
        );
    }

    private String buildPolicyApprovalBody(Map<String, String> vars) {
        return String.format(
                "Hello %s,\n\n" +
                "Great news! Your policy has been approved.\n" +
                "Policy Number: %s\n" +
                "Coverage Effective Date: %s\n" +
                "Annual Premium: %s\n\n" +
                "Your policy details are available in your FireShield dashboard.\n\n" +
                "Best regards,\nFireShield Insurance Team",
                vars.getOrDefault("customerName", "Valued Customer"),
                vars.getOrDefault("policyNumber", "N/A"),
                vars.getOrDefault("effectiveDate", "TBD"),
                vars.getOrDefault("premium", "Contact us for details")
        );
    }

    private String buildPolicySubmittedBody(Map<String, String> vars) {
        return String.format(
                "Hello %s,\n\n" +
                "Your policy application has been received successfully.\n" +
                "Reference: %s\n" +
                "Policy: %s\n\n" +
                "Our team will review your submission and notify you of the next steps.\n\n" +
                "Best regards,\nFireShield Insurance Team",
                vars.getOrDefault("customerName", "Valued Customer"),
                vars.getOrDefault("policyNumber", "N/A"),
                vars.getOrDefault("policyName", "N/A")
        );
    }

    private String buildPolicyRejectionBody(Map<String, String> vars) {
        return String.format(
                "Hello %s,\n\n" +
                "Your policy application has been reviewed.\n" +
                "Reason: %s\n\n" +
                "Please contact our support team at support@fireshield.local for more information.\n\n" +
                "Best regards,\nFireShield Insurance Team",
                vars.getOrDefault("customerName", "Valued Customer"),
                vars.getOrDefault("reason", "Additional information required")
        );
    }

    private String buildRenewalReminderBody(Map<String, String> vars) {
        return String.format(
                "Hello %s,\n\n" +
                "Your policy #%s will expire on %s.\n" +
                "Please renew your policy to maintain continuous coverage.\n\n" +
                "Visit your FireShield dashboard to renew your policy now.\n\n" +
                "Best regards,\nFireShield Insurance Team",
                vars.getOrDefault("customerName", "Valued Customer"),
                vars.getOrDefault("policyNumber", "N/A"),
                vars.getOrDefault("expirationDate", "Soon")
        );
    }

    private String buildDefaultBody(Map<String, String> vars) {
        return String.format(
                "Hello %s,\n\n" +
                "You have a new notification from FireShield Insurance.\n" +
                "Details: %s\n\n" +
                "Please log in to your dashboard for more information.\n\n" +
                "Best regards,\nFireShield Insurance Team",
                vars.getOrDefault("userName", "Valued Customer"),
                vars.getOrDefault("details", "Check your dashboard for updates")
        );
    }
}
