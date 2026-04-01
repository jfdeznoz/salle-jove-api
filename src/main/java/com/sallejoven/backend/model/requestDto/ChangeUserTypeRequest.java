package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChangeUserTypeRequest(
        @NotNull @Min(2) @Max(3) Integer userType
) {}
