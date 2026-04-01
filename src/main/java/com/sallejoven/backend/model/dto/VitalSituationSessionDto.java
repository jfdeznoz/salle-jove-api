package com.sallejoven.backend.model.dto;

public record VitalSituationSessionDto(
    Long id,
    Long vitalSituationId,
    String title,
    String pdf,
    Boolean isDefault
) {}
