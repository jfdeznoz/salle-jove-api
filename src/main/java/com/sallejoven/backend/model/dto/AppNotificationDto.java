package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sallejoven.backend.model.enums.AppNotificationReferenceType;
import com.sallejoven.backend.model.enums.AppNotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppNotificationDto(
        UUID uuid,
        AppNotificationType type,
        String title,
        String message,
        AppNotificationReferenceType referenceType,
        UUID referenceUuid,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime readAt
) {}
