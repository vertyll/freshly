package com.vertyll.freshly.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponseDto(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("refresh_expires_in") int refreshExpiresIn,
        @JsonProperty("token_type") String tokenType) {}
