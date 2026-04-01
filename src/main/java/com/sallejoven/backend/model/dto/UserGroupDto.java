package com.sallejoven.backend.model.dto;

public record UserGroupDto(
    Integer userType,
    Long groupId,
    Integer id,
    Integer stage
) {}
