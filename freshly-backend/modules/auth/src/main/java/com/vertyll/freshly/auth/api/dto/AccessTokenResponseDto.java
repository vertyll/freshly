package com.vertyll.freshly.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccessTokenResponseDto(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("token_type") String tokenType) {}
