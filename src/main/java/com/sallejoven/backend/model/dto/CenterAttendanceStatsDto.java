package com.sallejoven.backend.model.dto;

import java.util.List;

public record CenterAttendanceStatsDto(
        List<GroupStatsDto> groups,
        Double overallRate,
        Integer overallYellowWarnings,
        Integer overallRedWarnings
) {

    public record GroupStatsDto(
            java.util.UUID groupUuid,
            Integer stage,
            Integer sessionsCount,
            Double avgAttendanceRate,
            Integer yellowWarnings,
            Integer redWarnings
    ) {}
}
