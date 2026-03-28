package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.NotificationDTO;
import org.hartford.fireinsurance.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','SURVEYOR','UNDERWRITER','SIU_INVESTIGATOR')")
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            Authentication authentication,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        String username = authentication.getName();
        return ResponseEntity.ok(notificationService.getNotificationsForUser(username, limit));
    }
}