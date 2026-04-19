package com.sallejoven.backend.controller;

import com.sallejoven.backend.model.dto.AppNotificationDto;
import com.sallejoven.backend.model.dto.NotificationSummaryDto;
import com.sallejoven.backend.service.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/summary")
    public ResponseEntity<NotificationSummaryDto> getSummary() {
        return ResponseEntity.ok(notificationService.getSummary());
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<AppNotificationDto>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getPaged(page, size));
    }

    @PutMapping("/{notificationUuid}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationUuid) {
        notificationService.markAsRead(notificationUuid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }
}
