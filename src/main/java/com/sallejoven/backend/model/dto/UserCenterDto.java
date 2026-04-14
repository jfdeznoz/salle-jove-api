package com.sallejoven.backend.model.dto;

public record UserCenterDto(
    java.util.UUID uuid,
    java.util.UUID centerUuid,
    String centerName,
    String cityName,
    Integer userType
) {}
