package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ForkCenterRequest(
        @NotBlank String newName,
        @NotBlank @Size(max = 100) String newCity,
        @NotEmpty List<@NotBlank String> groupUuids
) {}
