package com.vertyll.freshly.auth.domain;

import com.vertyll.freshly.auth.domain.exception.InvalidVerificationTokenException;
import com.vertyll.freshly.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private JwtProperties.Expiration expiration;

    @InjectMocks
    private VerificationTokenService verificationTokenService;

    private static final String SECRET = "testSecretKeyForJwtTokensMinimum256BitsRequiredForHS256AlgorithmInTest==";
    private static final long EMAIL_VERIFICATION_EXPIRATION = 3600000L;
    private static final long PASSWORD_RESET_EXPIRATION = 1800000L;

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.secret()).thenReturn(SECRET);
        lenient().when(jwtProperties.expiration()).thenReturn(expiration);
        lenient().when(expiration.emailVerification()).thenReturn(EMAIL_VERIFICATION_EXPIRATION);
        lenient().when(expiration.passwordReset()).thenReturn(PASSWORD_RESET_EXPIRATION);
    }

    @Test
    void shouldGenerateEmailVerificationToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        // When
        String token = verificationTokenService.generateEmailVerificationToken(userId, email);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        // Verify token structure
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo(email);
        assertThat(claims.get("type", String.class)).isEqualTo("email_verification");
    }

    @Test
    void shouldGeneratePasswordResetToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        // When
        String token = verificationTokenService.generatePasswordResetToken(userId, email);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo(email);
        assertThat(claims.get("type", String.class)).isEqualTo("password_reset");
    }

    @Test
    void shouldValidateEmailVerificationToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = verificationTokenService.generateEmailVerificationToken(userId, email);

        // When
        UUID validatedUserId = verificationTokenService.validateEmailVerificationToken(token);

        // Then
        assertThat(validatedUserId).isEqualTo(userId);
    }

    @Test
    void shouldValidatePasswordResetToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = verificationTokenService.generatePasswordResetToken(userId, email);

        // When
        UUID validatedUserId = verificationTokenService.validatePasswordResetToken(token);

        // Then
        assertThat(validatedUserId).isEqualTo(userId);
    }

    @Test
    void shouldThrowException_whenTokenIsInvalid() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThatThrownBy(() -> verificationTokenService.validateEmailVerificationToken(invalidToken))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void shouldThrowException_whenTokenIsExpired() {
        // Given - Manual JWT with expired date (past)
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String expiredToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "test@example.com")
                .claim("type", "email_verification")
                .issuedAt(Date.from(now.minusSeconds(10)))
                .expiration(Date.from(now.minusSeconds(5)))
                .signWith(key)
                .compact();

        // When & Then
        assertThatThrownBy(() -> verificationTokenService.validateEmailVerificationToken(expiredToken))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessageContaining("expired");
    }
}
