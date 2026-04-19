package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotNull;

public record DefaultFlagRequest(
        @NotNull Boolean isDefault
) {
}
