package com.vertyll.freshly.auth.infrastructure.web.dto;

import com.vertyll.freshly.auth.application.dto.AuthTokens;

public record TokenResponseDto(String accessToken, String tokenType, long expiresInSeconds) {
    public static TokenResponseDto from(AuthTokens tokens) {
        return new TokenResponseDto(tokens.accessToken(), tokens.tokenType(), tokens.expiresInSeconds());
    }
}
