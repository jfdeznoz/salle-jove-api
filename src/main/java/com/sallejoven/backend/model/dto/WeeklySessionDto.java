package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record WeeklySessionDto(
    Long id,
    Long vitalSituationSessionId,
    String vitalSituationTitle,
    String vitalSituationSessionTitle,
    String title,
    Long groupId,
    String groupName,
    Integer stage,
    Long centerId,
    String centerName,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime sessionDateTime,
    Integer status
) {}
