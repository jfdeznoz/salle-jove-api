package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sallejoven.backend.model.enums.WeeklySessionWarningType;

import java.time.LocalDateTime;
import java.util.List;

public record UserAttendanceStatsDto(
        SessionAttendanceDto sessionAttendance,
        EventAttendanceDto eventAttendance,
        WarningStatsDto warnings,
        List<AcademicGroupDto> memberships,
        List<RecentSessionDto> recentSessions,
        List<LedSessionDto> ledSessions
) {

    public record SessionAttendanceDto(
            Integer total,
            Integer attended,
            Integer justified,
            Double rate
    ) {}

    public record EventAttendanceDto(
            Integer total,
            Integer attended,
            Double rate
    ) {}

    public record WarningStatsDto(
            Integer yellow,
            Integer red,
            Integer total
    ) {}

    public record AcademicGroupDto(
            String centerName,
            Integer stage,
            Integer userType
    ) {}

    public record RecentSessionDto(
            java.util.UUID sessionUuid,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime date,
            String title,
            String vitalSituationTitle,
            String vitalSituationSessionTitle,
            Boolean attended,
            Boolean justified,
            WeeklySessionWarningType warningType
    ) {}

    public record LedSessionDto(
            java.util.UUID sessionUuid,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime date,
            String title,
            String vitalSituationTitle,
            String vitalSituationSessionTitle,
            String content,
            String observations,
            String centerName,
            Integer stage
    ) {}
}
