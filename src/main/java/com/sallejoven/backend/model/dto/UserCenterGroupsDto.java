package com.sallejoven.backend.model.dto;

import java.util.List;

public record UserCenterGroupsDto(
    java.util.UUID centerUuid,
    String centerName,
    String cityName,
    List<UserGroupDto> groups,
    WeeklySessionSummaryDto weeklySessionSummary
) {
    public UserCenterGroupsDto(
            java.util.UUID centerUuid,
            String centerName,
            String cityName,
            List<UserGroupDto> groups) {
        this(centerUuid, centerName, cityName, groups, null);
    }
}
