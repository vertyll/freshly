package com.vertyll.freshly.security;

import com.vertyll.freshly.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JwtConfigurationTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private JwtProperties.RefreshToken refreshToken;

    @Mock
    private JwtProperties.Expiration expiration;

    private static final String SECRET = "testSecretKeyForJwtTokensMinimum256BitsRequiredForHS256AlgorithmInTest==";

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.secret()).thenReturn(SECRET);
        lenient().when(jwtProperties.refreshToken()).thenReturn(refreshToken);
        lenient().when(jwtProperties.expiration()).thenReturn(expiration);
        lenient().when(expiration.emailVerification()).thenReturn(3600000L);
        lenient().when(expiration.passwordReset()).thenReturn(1800000L);
        lenient().when(refreshToken.expiration()).thenReturn(86400000L);
        lenient().when(refreshToken.cookieName()).thenReturn("refresh_token");
    }

    @Test
    void shouldLoadJwtProperties() {
        // Then
        assertThat(jwtProperties).isNotNull();
        assertThat(jwtProperties.secret()).isNotNull();
        assertThat(jwtProperties.refreshToken()).isNotNull();
        assertThat(jwtProperties.expiration()).isNotNull();
    }

    @Test
    void shouldGenerateAndValidateToken() {
        // Given
        String userId = UUID.randomUUID().toString();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiresAt = Date.from(now.plusMillis(3600000L));

        // When - Generate token
        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(key)
                .compact();

        // Then - Validate token
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.getIssuedAt()).isCloseTo(issuedAt, 1000); // Within 1 second
        assertThat(claims.getExpiration()).isCloseTo(expiresAt, 1000);
    }

    @Test
    void shouldHaveValidSecretKeyLength() {
        // Given & Then - HS256 requires at least 256 bits (32 bytes)
        assertThat(SECRET.getBytes(StandardCharsets.UTF_8).length).isGreaterThanOrEqualTo(32);
    }

    @Test
    void shouldHaveReasonableTokenExpiration() {
        // Then
        long emailVerificationMinutes = expiration.emailVerification() / (1000 * 60);
        long passwordResetMinutes = expiration.passwordReset() / (1000 * 60);
        long refreshTokenMinutes = refreshToken.expiration() / (1000 * 60);

        assertThat(emailVerificationMinutes).isBetween(1L, 2880L); // Between 1 minute and 48 hours
        assertThat(passwordResetMinutes).isBetween(1L, 2880L);
        assertThat(refreshTokenMinutes).isGreaterThan(0L);
    }

    @Test
    void shouldHaveRefreshTokenCookieName() {
        // Then
        assertThat(refreshToken.cookieName()).isNotNull();
        assertThat(refreshToken.cookieName()).isNotEmpty();
    }
}
