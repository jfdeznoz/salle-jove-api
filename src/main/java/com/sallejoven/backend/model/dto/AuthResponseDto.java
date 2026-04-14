package com.sallejoven.backend.model.dto;

import com.sallejoven.backend.model.enums.TokenType;

public record AuthResponseDto(
    String accessToken,
    int accessTokenExpiry,
    TokenType tokenType,
    String userName
) {}
