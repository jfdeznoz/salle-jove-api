package com.sallejoven.backend.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.sallejoven.backend.model.enums.TokenType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDto {

    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("access_token_expiry")
    private int accessTokenExpiry;

    @JsonProperty("token_type")
    private TokenType tokenType;
    
    @JsonProperty("user_name")
    private String userName;

}