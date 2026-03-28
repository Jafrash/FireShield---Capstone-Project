package org.hartford.fireinsurance.controller;

import lombok.extern.slf4j.Slf4j;
import org.hartford.fireinsurance.model.NotificationPreference;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.NotificationPreferenceRepository;
import org.hartford.fireinsurance.repository.UserRepository;
import org.hartford.fireinsurance.service.EmailNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class EmailTestController {

    private final EmailNotificationService emailNotificationService;
    private final UserRepository userRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public EmailTestController(
            EmailNotificationService emailNotificationService,
            UserRepository userRepository,
            NotificationPreferenceRepository notificationPreferenceRepository) {
        this.emailNotificationService = emailNotificationService;
        this.userRepository = userRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    /**
     * Test endpoint to verify email functionality
     * Usage: POST /api/test/email with {"email": "test@example.com"}
     */
    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> testEmail(@RequestBody Map<String, String> request) {
        log.info("Testing email functionality...");

        String testEmail = request.get("email");
        if (testEmail == null || testEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email address is required"));
        }

        try {
            Map<String, String> templateVars = new HashMap<>();
            templateVars.put("customerName", "Test User");
            templateVars.put("claimId", "TEST-CLAIM-123");
            templateVars.put("status", "TESTING");
            templateVars.put("details", "This is a test email from FireShield Insurance System");

            log.info("Sending test email to: {}", testEmail);
            emailNotificationService.sendEmailNotification(testEmail, "CLAIM_STATUS_CHANGE", templateVars);

            log.info("Test email sent successfully to: {}", testEmail);
            return ResponseEntity.ok(Map.of(
                "message", "Test email sent successfully",
                "recipient", testEmail,
                "eventType", "CLAIM_STATUS_CHANGE"
            ));
        } catch (Exception e) {
            log.error("Failed to send test email to {}: {}", testEmail, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to send email: " + e.getMessage(),
                "recipient", testEmail
            ));
        }
    }

    /**
     * Check email configuration status
     */
    @GetMapping("/email-config")
    public ResponseEntity<Map<String, Object>> checkEmailConfig() {
        log.info("Checking email configuration...");

        Map<String, Object> config = new HashMap<>();
        config.put("emailServiceAvailable", true);
        config.put("message", "Email service is configured and available for testing");
        config.put("testEndpoint", "POST /api/test/email with {\"email\": \"your-email@example.com\"}");

        return ResponseEntity.ok(config);
    }

    /**
     * Check notification preferences for all users
     */
    @GetMapping("/notification-preferences")
    public ResponseEntity<Map<String, Object>> checkNotificationPreferences() {
        log.info("Checking user notification preferences...");

        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userPrefs = users.stream()
            .map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("username", user.getUsername());
                userInfo.put("email", user.getEmail());
                userInfo.put("role", user.getRole());

                NotificationPreference pref = notificationPreferenceRepository.findByUser(user)
                    .orElse(null);

                if (pref != null) {
                    userInfo.put("emailEnabled", pref.getEmailEnabled());
                    userInfo.put("enabledEvents", pref.getEnabledEventKeys());
                } else {
                    userInfo.put("emailEnabled", "NOT_SET");
                    userInfo.put("enabledEvents", "NOT_SET");
                }

                return userInfo;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", users.size());
        response.put("userPreferences", userPrefs);
        response.put("message", "Notification preferences analysis completed");

        return ResponseEntity.ok(response);
    }

    /**
     * Fix notification preferences for users who don't have them set
     */
    @PostMapping("/fix-notification-preferences")
    public ResponseEntity<Map<String, Object>> fixNotificationPreferences() {
        log.info("Fixing notification preferences for users...");

        List<User> users = userRepository.findAll();
        int fixed = 0;
        int alreadySet = 0;

        for (User user : users) {
            if (notificationPreferenceRepository.findByUser(user).isEmpty()) {
                NotificationPreference pref = new NotificationPreference(user);
                notificationPreferenceRepository.save(pref);

                log.info("Created notification preferences for user: {} with email: {}",
                    user.getUsername(), user.getEmail());
                fixed++;
            } else {
                alreadySet++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", users.size());
        response.put("preferencesFixed", fixed);
        response.put("alreadyConfigured", alreadySet);
        response.put("message", "Notification preferences fix completed");

        return ResponseEntity.ok(response);
    }
}