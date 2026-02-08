package com.vertyll.freshly.auth.domain;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.auth.domain.exception.InvalidVerificationTokenException;
import com.vertyll.freshly.common.config.JwtProperties;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private static final String EMAIL_CLAIM = "email";
    private static final String TYPE_CLAIM = "type";
    private static final String EMAIL_VERIFICATION_TYPE = "email_verification";
    private static final String PASSWORD_RESET_TYPE = "password_reset";

    private static final String ERROR_TOKEN_INVALID_MSG_KEY = "error.auth.tokenInvalid";
    private static final String ERROR_TOKEN_EXPIRED_MSG_KEY = "error.auth.tokenExpired";
    private static final String ERROR_TOKEN_TYPE_INVALID_MSG_KEY = "error.auth.tokenTypeInvalid";
    private static final String ERROR_TOKEN_FORMAT_INVALID_MSG_KEY =
            "error.auth.tokenFormatInvalid";
    private static final String ERROR_PASSWORD_RESET_TOKEN_EXPIRED_MSG_KEY =
            "error.auth.passwordResetTokenExpired";
    private static final String ERROR_PASSWORD_RESET_TOKEN_INVALID_MSG_KEY =
            "error.auth.passwordResetTokenInvalid";

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(Charset.defaultCharset()));
    }

    public String generateEmailVerificationToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.expiration().emailVerification());

        return Jwts.builder()
                .subject(userId.toString())
                .claim(EMAIL_CLAIM, email)
                .claim(TYPE_CLAIM, EMAIL_VERIFICATION_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generatePasswordResetToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.expiration().passwordReset());

        return Jwts.builder()
                .subject(userId.toString())
                .claim(EMAIL_CLAIM, email)
                .claim(TYPE_CLAIM, PASSWORD_RESET_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public UUID validateEmailVerificationToken(String token) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(getSigningKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

            String type = claims.get(TYPE_CLAIM, String.class);
            if (!EMAIL_VERIFICATION_TYPE.equals(type)) {
                throw new InvalidVerificationTokenException(ERROR_TOKEN_TYPE_INVALID_MSG_KEY);
            }

            String userId = claims.getSubject();
            return UUID.fromString(userId);

        } catch (ExpiredJwtException _) {
            throw new InvalidVerificationTokenException(ERROR_TOKEN_EXPIRED_MSG_KEY);
        } catch (JwtException _) {
            throw new InvalidVerificationTokenException(ERROR_TOKEN_INVALID_MSG_KEY);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID in token: {}", e.getMessage());
            throw new InvalidVerificationTokenException(ERROR_TOKEN_FORMAT_INVALID_MSG_KEY);
        }
    }

    public UUID validatePasswordResetToken(String token) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(getSigningKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

            String type = claims.get(TYPE_CLAIM, String.class);
            if (!PASSWORD_RESET_TYPE.equals(type)) {
                throw new InvalidVerificationTokenException(ERROR_TOKEN_TYPE_INVALID_MSG_KEY);
            }

            String userId = claims.getSubject();
            return UUID.fromString(userId);

        } catch (ExpiredJwtException e) {
            log.warn("Password reset token expired: {}", e.getMessage());
            throw new InvalidVerificationTokenException(ERROR_PASSWORD_RESET_TOKEN_EXPIRED_MSG_KEY);
        } catch (JwtException e) {
            log.warn("Invalid password reset token: {}", e.getMessage());
            throw new InvalidVerificationTokenException(ERROR_PASSWORD_RESET_TOKEN_INVALID_MSG_KEY);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID in token: {}", e.getMessage());
            throw new InvalidVerificationTokenException(ERROR_TOKEN_FORMAT_INVALID_MSG_KEY);
        }
    }

    @Nullable
    public String extractEmail(String token) {
        try {
            Claims claims =
                    Jwts.parser()
                            .verifyWith(getSigningKey())
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

            return claims.get(EMAIL_CLAIM, String.class);
        } catch (JwtException _) {
            return null;
        }
    }
}
