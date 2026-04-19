package com.sallejoven.backend.model.dto;

import java.util.List;

public record AdminOverviewDto(
        List<CenterOverviewDto> centers,
        List<CenterOverviewDto> topCenters,
        List<CenterOverviewDto> bottomCenters,
        Double globalSessionRate,
        Double globalEventRate,
        Integer totalYellowWarnings,
        Integer totalRedWarnings,
        Integer totalUsers,
        Integer totalSessions,
        Integer totalEvents
) {

    public record CenterOverviewDto(
            java.util.UUID uuid,
            String name,
            String city,
            Integer groupCount,
            Integer memberCount,
            Double sessionRate,
            Double eventRate,
            Integer yellowWarnings,
            Integer redWarnings
    ) {}
}
