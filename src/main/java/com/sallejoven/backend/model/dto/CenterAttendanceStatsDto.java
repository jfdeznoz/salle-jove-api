package com.sallejoven.backend.model.dto;

import java.util.List;

public record CenterAttendanceStatsDto(
        List<GroupStatsDto> groups,
        Double overallRate
) {

    public record GroupStatsDto(
            java.util.UUID groupUuid,
            Integer stage,
            Integer sessionsCount,
            Double avgAttendanceRate
    ) {}
}
