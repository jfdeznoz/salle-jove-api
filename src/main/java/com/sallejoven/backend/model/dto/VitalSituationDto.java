package com.sallejoven.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VitalSituationDto {
    private Long id;
    private String title;
    private Integer[] stages;
    private Boolean isDefault;
}

