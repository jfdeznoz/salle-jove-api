package com.sallejoven.backend.model.dto;

public record VitalSituationDto(
    Long id,
    String title,
    Integer[] stages,
    Boolean isDefault
) {}
