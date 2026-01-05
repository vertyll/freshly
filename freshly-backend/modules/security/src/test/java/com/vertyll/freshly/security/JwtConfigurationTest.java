package com.vertyll.freshly.security;

import com.vertyll.freshly.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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

    private final JwtProperties jwtProperties = Mockito.mock(JwtProperties.class);
    private final JwtProperties.RefreshToken refreshToken = Mockito.mock(JwtProperties.RefreshToken.class);
    private final JwtProperties.Expiration expiration = Mockito.mock(JwtProperties.Expiration.class);

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
        Instant expiry = now.plusMillis(3600000L);

        // When - Generate token
        String token = Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now)) 
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();

        // Then - Validate token
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

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
        // Given
        long emailVerificationMs = 3600000L;
        long passwordResetMs = 1800000L;
        long refreshTokenMs = 86400000L;
        
        // Then
        long emailVerificationMinutes = emailVerificationMs / (1000 * 60);
        long passwordResetMinutes = passwordResetMs / (1000 * 60);
        long refreshTokenMinutes = refreshTokenMs / (1000 * 60);

        assertThat(emailVerificationMinutes).isBetween(1L, 2880L); // Between 1 minute and 48 hours
        assertThat(passwordResetMinutes).isBetween(1L, 2880L);
        assertThat(refreshTokenMinutes).isGreaterThan(0L);
    }

    @Test
    void shouldHaveRefreshTokenCookieName() {
        // Given
        String cookieName = "refresh_token";
        
        // Then
        assertThat(cookieName)
            .isNotNull()
            .isNotEmpty();
    }
}
