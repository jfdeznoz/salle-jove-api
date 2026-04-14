package com.sallejoven.backend.model.dto;

public record UserGroupDto(
    Integer userType,
    java.util.UUID groupUuid,
    java.util.UUID uuid,
    Integer stage
) {}
