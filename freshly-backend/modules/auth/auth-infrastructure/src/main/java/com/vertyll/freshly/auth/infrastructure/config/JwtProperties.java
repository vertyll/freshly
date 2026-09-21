package com.vertyll.freshly.auth.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "application.jwt")
public record JwtProperties(String secret, Expiration expiration) {

    public JwtProperties {
        requireNonNull(secret, "application.jwt.secret must be configured");
        requireNonNull(expiration, "application.jwt.expiration must be configured");
    }

    @Override
    public String toString() {
        return "JwtProperties[secret=***, expiration=" + expiration + "]";
    }

    public record Expiration(Duration emailVerification, Duration passwordReset) {
        public Expiration {
            requireNonNull(emailVerification, "application.jwt.expiration.email-verification must be configured");
            requireNonNull(passwordReset, "application.jwt.expiration.password-reset must be configured");
        }
    }
}
