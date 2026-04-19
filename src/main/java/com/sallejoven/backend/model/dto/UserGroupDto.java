package com.sallejoven.backend.model.dto;

public record UserGroupDto(
    Integer userType,
    java.util.UUID groupUuid,
    java.util.UUID uuid,
    Integer stage,
    WeeklySessionSummaryDto weeklySessionSummary
) {
    public UserGroupDto(
            Integer userType,
            java.util.UUID groupUuid,
            java.util.UUID uuid,
            Integer stage) {
        this(userType, groupUuid, uuid, stage, null);
    }
}
