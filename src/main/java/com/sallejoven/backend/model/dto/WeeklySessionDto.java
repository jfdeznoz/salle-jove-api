package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record WeeklySessionDto(
    java.util.UUID uuid,
    java.util.UUID vitalSituationSessionUuid,
    String vitalSituationTitle,
    String vitalSituationSessionTitle,
    String title,
    java.util.UUID groupUuid,
    String groupName,
    Integer stage,
    java.util.UUID centerUuid,
    String centerName,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime sessionDateTime,
    String observations,
    String content,
    Integer attendanceCount,
    Integer totalCount,
    Integer status
) {}
