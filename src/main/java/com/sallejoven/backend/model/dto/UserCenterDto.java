package com.sallejoven.backend.model.dto;

public record UserCenterDto(
    Long id,
    Long centerId,
    String centerName,
    String cityName,
    Integer userType
) {}
