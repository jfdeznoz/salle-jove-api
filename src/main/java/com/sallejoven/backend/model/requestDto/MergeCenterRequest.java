package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotBlank;

public record MergeCenterRequest(
        @NotBlank String sourceCenterUuid
) {}
