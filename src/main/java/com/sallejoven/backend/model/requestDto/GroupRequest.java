package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GroupRequest(
        @Positive Long centerId,
        @NotNull @Min(0) @Max(8) Integer stage
) {}
