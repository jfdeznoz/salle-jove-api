package com.sallejoven.backend.model.dto;

public record GroupDto(
    Long id,
    Long centerId,
    Integer stage,
    String centerName,
    String cityName
) {}
