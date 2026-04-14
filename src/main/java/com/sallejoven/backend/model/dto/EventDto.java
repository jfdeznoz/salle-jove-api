package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record EventDto(
    java.util.UUID uuid,
    String name,
    String description,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate eventDate,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
    String fileName,
    String place,
    Integer[] stages,
    Boolean isGeneral,
    Boolean isBlocked,
    java.util.UUID centerUuid,
    String centerName,
    String pdf
) {}
