package com.vertyll.freshly.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vertyll.freshly.security.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class JwtConfigurationTest {
    private static final long EXPIRATION_EMAIL_VERIFICATION = 3600000L;
    private static final long EXPIRATION_PASSWORD_RESET = 1800000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 86400000L;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String SECRET = "testSecretKeyForJwtTokensMinimum256BitsRequiredForHS256";

    private final JwtProperties jwtProperties = Mockito.mock(JwtProperties.class);
    private final JwtProperties.RefreshToken refreshToken =
            Mockito.mock(JwtProperties.RefreshToken.class);
    private final JwtProperties.Expiration expiration =
            Mockito.mock(JwtProperties.Expiration.class);

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.secret()).thenReturn(SECRET);
        lenient().when(jwtProperties.refreshToken()).thenReturn(refreshToken);
        lenient().when(jwtProperties.expiration()).thenReturn(expiration);
        lenient().when(expiration.emailVerification()).thenReturn(EXPIRATION_EMAIL_VERIFICATION);
        lenient().when(expiration.passwordReset()).thenReturn(EXPIRATION_PASSWORD_RESET);
        lenient().when(refreshToken.expiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);
        lenient().when(refreshToken.cookieName()).thenReturn(REFRESH_TOKEN_COOKIE_NAME);
    }

    @Test
    void shouldLoadJwtProperties() {
        // Given & Then
        assertThat(jwtProperties).isNotNull();
        assertThat(SECRET).isNotNull();
        assertThat(refreshToken).isNotNull();
        assertThat(expiration).isNotNull();
    }

    @Test
    void shouldGenerateAndValidateToken() {
        // Given
        String userId = UUID.randomUUID().toString();
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        Instant now = Instant.now();
        Instant expiry = now.plusMillis(EXPIRATION_EMAIL_VERIFICATION);

        // When - Generate token
        String token =
                Jwts.builder()
                        .subject(userId)
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(expiry))
                        .signWith(key)
                        .compact();

        // Then - Validate token
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId);

        assertThat(claims.getIssuedAt()).isCloseTo(Date.from(now), 1000);
        assertThat(claims.getExpiration()).isCloseTo(Date.from(expiry), 1000);
    }

    @Test
    void shouldHaveValidSecretKeyLength() {
        // Given & Then - HS256 requires at least 256 bits (32 bytes)
        assertThat(SECRET.getBytes(StandardCharsets.UTF_8)).hasSizeGreaterThanOrEqualTo(32);
    }

    @Test
    void shouldHaveReasonableTokenExpiration() {
        // Given & Then
        long emailVerificationMinutes = EXPIRATION_EMAIL_VERIFICATION / (1000 * 60);
        long passwordResetMinutes = EXPIRATION_PASSWORD_RESET / (1000 * 60);
        long refreshTokenMinutes = REFRESH_TOKEN_EXPIRATION / (1000 * 60);

        assertThat(emailVerificationMinutes).isBetween(1L, 2880L); // Between 1 minute and 48 hours
        assertThat(passwordResetMinutes).isBetween(1L, 2880L);
        assertThat(refreshTokenMinutes).isGreaterThan(0L);
    }

    @Test
    void shouldHaveRefreshTokenCookieName() {
        // Given & Then
        assertThat(REFRESH_TOKEN_COOKIE_NAME).isNotNull().isNotEmpty();
    }
}
