package com.vertyll.freshly.auth.application.dto;

import org.jspecify.annotations.Nullable;

public record AuthTokens(
    String accessToken,
    @Nullable String refreshToken,
    String tokenType,
    long expiresInSeconds,
    long refreshExpiresInSeconds
) {

    @Override
    public String toString() {
        return "AuthTokens[accessToken=***, refreshToken=***, tokenType=" + tokenType + ", expiresInSeconds="
                + expiresInSeconds + ", refreshExpiresInSeconds=" + refreshExpiresInSeconds + "]";
    }
}
