package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSituationRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotEmpty
    private Integer[] stages;

    private Boolean isDefault;
}
