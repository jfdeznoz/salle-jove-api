package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record EventDto(
    Long id,
    String name,
    String description,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate eventDate,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
    String fileName,
    String place,
    Integer[] stages,
    Boolean isGeneral,
    Boolean isBlocked,
    Long centerId,
    String centerName,
    String pdf
) {}
