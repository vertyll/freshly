package com.vertyll.freshly.auth.domain;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.lenient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vertyll.freshly.auth.domain.exception.InvalidVerificationTokenException;
import com.vertyll.freshly.common.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    private static final long EMAIL_VERIFICATION_EXPIRATION = 3600000L;
    private static final long PASSWORD_RESET_EXPIRATION = 1800000L;

    private static final String SECRET = "testSecretKeyForJwtTokensMinimum256BitsRequiredForHS256";

    private static final String ERROR_TOKEN_EXPIRED_MSG_KEY = "error.auth.tokenExpired";
    private static final String ERROR_TOKEN_INVALID_MSG_KEY = "error.auth.tokenTypeInvalid";

    private static final String EMAIL_CLAIM_KEY = "email";
    private static final String TYPE_CLAIM_KEY = "type";
    private static final String TYPE_EMAIL_VERIFICATION = "email_verification";
    private static final String TYPE_PASSWORD_RESET = "password_reset";

    private static final String TEST_EMAIL = "test@example.com";

    @Mock private JwtProperties jwtProperties;
    @Mock private JwtProperties.Expiration expiration;

    @InjectMocks private VerificationTokenService verificationTokenService;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        lenient().when(jwtProperties.secret()).thenReturn(SECRET);
        lenient().when(jwtProperties.expiration()).thenReturn(expiration);
        lenient().when(expiration.emailVerification()).thenReturn(EMAIL_VERIFICATION_EXPIRATION);
        lenient().when(expiration.passwordReset()).thenReturn(PASSWORD_RESET_EXPIRATION);
    }

    @Test
    @DisplayName("Should generate valid email verification token")
    void shouldGenerateEmailVerificationToken() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        String token = verificationTokenService.generateEmailVerificationToken(userId, TEST_EMAIL);

        // Then
        assertThat(token).isNotNull().isNotEmpty();

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get(EMAIL_CLAIM_KEY, String.class)).isEqualTo(TEST_EMAIL);
        assertThat(claims.get(TYPE_CLAIM_KEY, String.class)).isEqualTo(TYPE_EMAIL_VERIFICATION);
    }

    @Test
    @DisplayName("Should generate valid password reset token")
    void shouldGeneratePasswordResetToken() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        String token = verificationTokenService.generatePasswordResetToken(userId, TEST_EMAIL);

        // Then
        assertThat(token).isNotNull().isNotEmpty();

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get(EMAIL_CLAIM_KEY, String.class)).isEqualTo(TEST_EMAIL);
        assertThat(claims.get(TYPE_CLAIM_KEY, String.class)).isEqualTo(TYPE_PASSWORD_RESET);
    }

    @Test
    @DisplayName("Should validate correct email verification token")
    void shouldValidateEmailVerificationToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String token = verificationTokenService.generateEmailVerificationToken(userId, TEST_EMAIL);

        // When
        UUID validatedUserId = verificationTokenService.validateEmailVerificationToken(token);

        // Then
        assertThat(validatedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should validate correct password reset token")
    void shouldValidatePasswordResetToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String token = verificationTokenService.generatePasswordResetToken(userId, TEST_EMAIL);

        // When
        UUID validatedUserId = verificationTokenService.validatePasswordResetToken(token);

        // Then
        assertThat(validatedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should throw exception when token is malformed")
    void shouldThrowExceptionWhenTokenIsInvalid() {
        // Given & When & Then
        assertThatThrownBy(
                        () ->
                                verificationTokenService.validateEmailVerificationToken(
                                        ERROR_TOKEN_INVALID_MSG_KEY))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    @DisplayName("Should throw exception and specific message key when token is expired")
    void shouldThrowExceptionWhenTokenIsExpired() {
        // Given
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String expiredToken =
                Jwts.builder()
                        .subject(UUID.randomUUID().toString())
                        .claim(EMAIL_CLAIM_KEY, TEST_EMAIL)
                        .claim(TYPE_CLAIM_KEY, TYPE_EMAIL_VERIFICATION)
                        .issuedAt(Date.from(now.minusSeconds(10)))
                        .expiration(Date.from(now.minusSeconds(5)))
                        .signWith(key)
                        .compact();

        // When & Then
        assertThatThrownBy(
                        () -> verificationTokenService.validateEmailVerificationToken(expiredToken))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessageContaining(ERROR_TOKEN_EXPIRED_MSG_KEY);
    }

    @Test
    @DisplayName("Should throw exception when token type is incorrect")
    void shouldThrowExceptionWhenTokenTypeIsMismatch() {
        // Given
        UUID userId = UUID.randomUUID();
        String resetToken = verificationTokenService.generatePasswordResetToken(userId, TEST_EMAIL);

        // When & Then
        assertThatThrownBy(
                        () -> verificationTokenService.validateEmailVerificationToken(resetToken))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessageContaining(ERROR_TOKEN_INVALID_MSG_KEY);
    }
}
