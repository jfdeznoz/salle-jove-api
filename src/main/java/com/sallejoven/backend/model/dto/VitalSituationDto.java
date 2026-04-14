package com.sallejoven.backend.model.dto;

public record VitalSituationDto(
    java.util.UUID uuid,
    String title,
    Integer[] stages,
    Boolean isDefault
) {}
