package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sallejoven.backend.model.enums.WeeklySessionWarningType;

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
    Integer yellowWarningCount,
    Integer redWarningCount,
    Integer attendanceCount,
    Integer totalCount,
    Integer status,
    Integer currentUserAttendanceStatus,
    Boolean currentUserJustified,
    WeeklySessionWarningType currentUserWarningType
) {}
