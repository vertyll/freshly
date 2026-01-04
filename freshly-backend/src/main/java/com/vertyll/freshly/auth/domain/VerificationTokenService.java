package com.vertyll.freshly.auth.domain;

import com.vertyll.freshly.auth.domain.exception.InvalidVerificationTokenException;
import com.vertyll.freshly.security.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private static final String EMAIL_CLAIM = "email";

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
                .claim("type", "email_verification")
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
                .claim("type", "password_reset")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public UUID validateEmailVerificationToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"email_verification".equals(type)) {
                throw new InvalidVerificationTokenException("Invalid token type");
            }

            String userId = claims.getSubject();
            return UUID.fromString(userId);

        } catch (ExpiredJwtException e) {
            log.warn("Email verification token expired: {}", e.getMessage());
            throw new InvalidVerificationTokenException("Verification token has expired. Please request a new one.");
        } catch (JwtException e) {
            log.warn("Invalid email verification token: {}", e.getMessage());
            throw new InvalidVerificationTokenException("Invalid verification token");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID in token: {}", e.getMessage());
            throw new InvalidVerificationTokenException("Invalid token format");
        }
    }

    public UUID validatePasswordResetToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"password_reset".equals(type)) {
                throw new InvalidVerificationTokenException("Invalid token type");
            }

            String userId = claims.getSubject();
            return UUID.fromString(userId);

        } catch (ExpiredJwtException e) {
            log.warn("Password reset token expired: {}", e.getMessage());
            throw new InvalidVerificationTokenException("Password reset token has expired. Please request a new one.");
        } catch (JwtException e) {
            log.warn("Invalid password reset token: {}", e.getMessage());
            throw new InvalidVerificationTokenException("Invalid password reset token");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID in token: {}", e.getMessage());
            throw new InvalidVerificationTokenException("Invalid token format");
        }
    }

    public String extractEmail(String token) {
        try {
            Claims claims = Jwts.parser()
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
