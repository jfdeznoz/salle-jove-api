package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddUserToGroupRequest(
        @NotBlank String userUuid,
        @NotNull @Min(0) @Max(5) Integer userType
) {}
