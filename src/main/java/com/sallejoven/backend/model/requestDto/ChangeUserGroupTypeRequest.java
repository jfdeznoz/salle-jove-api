package com.sallejoven.backend.model.requestDto;

import jakarta.validation.constraints.NotNull;

public record ChangeUserGroupTypeRequest(
        @NotNull Integer userType
) {}
