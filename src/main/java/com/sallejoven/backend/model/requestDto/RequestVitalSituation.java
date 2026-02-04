package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestVitalSituation {

    private Long id;

    @NotNull
    private String title;

    @NotNull
    private Integer[] stages;

    private Boolean isDefault;
}

