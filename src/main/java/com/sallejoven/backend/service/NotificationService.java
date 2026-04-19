package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.dto.AppNotificationDto;
import com.sallejoven.backend.model.dto.NotificationSummaryDto;
import com.sallejoven.backend.model.entity.AppNotification;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.AppNotificationReferenceType;
import com.sallejoven.backend.model.enums.AppNotificationType;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.repository.AppNotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AppNotificationRepository appNotificationRepository;
    private final AuthService authService;

    @Transactional
    public void createNotifications(List<UserSalle> recipients,
                                    AppNotificationType type,
                                    String title,
                                    String message,
                                    AppNotificationReferenceType referenceType,
                                    UUID referenceUuid) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        List<AppNotification> notifications = recipients.stream()
                .map(recipient -> AppNotification.builder()
                        .recipientUser(recipient)
                        .type(type)
                        .title(title)
                        .message(message)
                        .referenceType(referenceType)
                        .referenceUuid(referenceUuid)
                        .build())
                .toList();
        appNotificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public NotificationSummaryDto getSummary() {
        UserSalle currentUser = authService.getCurrentUser();
        long unreadCount = appNotificationRepository.countUnreadByRecipient(currentUser.getUuid());
        List<AppNotificationDto> items = appNotificationRepository.findUnreadByRecipient(
                        currentUser.getUuid(),
                        PageRequest.of(0, 5))
                .stream()
                .map(this::toDto)
                .toList();
        return new NotificationSummaryDto(unreadCount, items);
    }

    @Transactional(readOnly = true)
    public Page<AppNotificationDto> getPaged(int page, int size) {
        UserSalle currentUser = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return appNotificationRepository.findPagedByRecipient(currentUser.getUuid(), pageable)
                .map(this::toDto);
    }

    @Transactional
    public void markAsRead(UUID notificationUuid) {
        UserSalle currentUser = authService.getCurrentUser();
        AppNotification notification = appNotificationRepository.findOwnedByUuid(notificationUuid, currentUser.getUuid())
                .orElseThrow(() -> new SalleException(ErrorCodes.NOTIFICATION_NOT_FOUND));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            appNotificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead() {
        UserSalle currentUser = authService.getCurrentUser();
        appNotificationRepository.markAllAsRead(currentUser.getUuid(), LocalDateTime.now());
    }

    private AppNotificationDto toDto(AppNotification notification) {
        return new AppNotificationDto(
                notification.getUuid(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceUuid(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
