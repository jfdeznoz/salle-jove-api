package com.sallejoven.backend.model.dto;

public record VitalSituationSessionDto(
    java.util.UUID uuid,
    java.util.UUID vitalSituationUuid,
    String title,
    String pdf,
    Boolean isDefault
) {}
