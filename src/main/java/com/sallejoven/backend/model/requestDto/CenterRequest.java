package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CenterRequest(
        @NotBlank String name,
        @NotBlank @Size(max = 100) String city
) {}
