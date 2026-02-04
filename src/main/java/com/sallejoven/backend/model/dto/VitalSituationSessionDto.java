package com.sallejoven.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VitalSituationSessionDto {
    private Long id;
    private Long vitalSituationId;
    private String title;
    private String pdf;
    private Boolean isDefault;
}

