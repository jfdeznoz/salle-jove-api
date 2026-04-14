package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GroupRequest(
        @NotBlank String centerUuid,
        @NotNull @Min(0) @Max(8) Integer stage
) {}
