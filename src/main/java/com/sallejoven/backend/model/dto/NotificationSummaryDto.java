package com.sallejoven.backend.model.dto;

import java.util.List;

public record NotificationSummaryDto(
        Long unreadCount,
        List<AppNotificationDto> items
) {}
