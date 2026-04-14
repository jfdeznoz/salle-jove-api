package com.sallejoven.backend.model.dto;

public record GroupDto(
    java.util.UUID uuid,
    java.util.UUID centerUuid,
    Integer stage,
    String centerName,
    String cityName
) {}
