package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record UserAttendanceStatsDto(
        SessionAttendanceDto sessionAttendance,
        EventAttendanceDto eventAttendance,
        List<RecentSessionDto> recentSessions
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

    public record RecentSessionDto(
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime date,
            String title,
            String vitalSituationTitle,
            String vitalSituationSessionTitle,
            Boolean attended,
            Boolean justified
    ) {}
}
