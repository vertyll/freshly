package com.vertyll.freshly.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "security.jwt")
@Validated
public record JwtProperties(
        String secret,
        Expiration expiration
) {
    public record Expiration(
            long emailVerification,
            long passwordReset
    ) {
    }
}
