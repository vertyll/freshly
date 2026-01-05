package com.vertyll.freshly.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "security.jwt")
@Validated
public record JwtProperties(String secret, RefreshToken refreshToken, Expiration expiration) {
    public record RefreshToken(long expiration, String cookieName) {}

    public record Expiration(long emailVerification, long passwordReset) {}
}
