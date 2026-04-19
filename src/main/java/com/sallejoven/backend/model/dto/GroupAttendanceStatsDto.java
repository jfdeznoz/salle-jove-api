package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record GroupAttendanceStatsDto(
        List<SessionSummaryDto> sessions,
        List<MemberAttendanceDto> members
) {

    public record SessionSummaryDto(
            java.util.UUID uuid,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime date,
            String title,
            String vitalSituationTitle,
            String content,
            Integer yellowWarnings,
            Integer redWarnings,
            Integer attendanceCount,
            Integer totalCount
    ) {}

    public record MemberAttendanceDto(
            java.util.UUID userUuid,
            String name,
            String lastName,
            Integer yellowWarnings,
            Integer redWarnings,
            Integer sessionsAttended,
            Integer sessionsTotal,
            Double rate
    ) {}
}
