package com.sallejoven.backend.model.dto;

public record WeeklySessionSummaryDto(
        Integer currentWeekCount,
        Integer previousWeekCount
) {}
