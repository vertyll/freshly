package com.vertyll.freshly.security;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenTypeValidatorTest {
    private final OAuth2TokenValidator<Jwt> validator = new SecurityConfig().accessTokenTypeValidator();

    @Test
    @DisplayName("accepts a Keycloak access token")
    void acceptsAnAccessToken() {
        assertThat(validator.validate(tokenOfType("Bearer")).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("refuses a Keycloak ID token")
    void refusesAnIdToken() {
        assertThat(validator.validate(tokenOfType("ID")).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("refuses a token that names no type")
    void refusesAnUntypedToken() {
        Jwt untyped = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("ada")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();

        assertThat(validator.validate(untyped).hasErrors()).isTrue();
    }

    private static Jwt tokenOfType(String type) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("ada")
            .claim("typ", type)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    }
}
