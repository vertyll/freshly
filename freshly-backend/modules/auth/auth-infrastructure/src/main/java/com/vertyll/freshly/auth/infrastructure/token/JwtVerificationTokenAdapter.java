package com.vertyll.freshly.auth.infrastructure.token;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.auth.application.port.outbound.VerificationTokenPort;
import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.auth.domain.model.TokenPurpose;
import com.vertyll.freshly.auth.domain.model.VerificationToken;
import com.vertyll.freshly.auth.infrastructure.config.JwtProperties;
import com.vertyll.freshly.lang.error.DomainException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public final class JwtVerificationTokenAdapter implements VerificationTokenPort {
    private static final String EMAIL_CLAIM = "email";
    private static final String PURPOSE_CLAIM = "type";

    private final SecretKey signingKey;
    private final JwtProperties properties;

    public JwtVerificationTokenAdapter(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String issue(UUID userId, String email, TokenPurpose purpose) {
        Instant now = Instant.now();
        Instant expiry = now.plus(lifetimeOf(purpose));

        return Jwts.builder()
            .subject(userId.toString())
            .claim(EMAIL_CLAIM, email)
            .claim(PURPOSE_CLAIM, purpose.claimValue())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact();
    }

    @Override
    public VerificationToken validate(String token, TokenPurpose expectedPurpose) {
        Claims claims = parse(token);

        String purpose = claims.get(PURPOSE_CLAIM, String.class);
        if (!expectedPurpose.claimValue().equals(purpose)) {
            log.warn("Token presented for {} but issued for {}", expectedPurpose, purpose);
            throw new DomainException(AuthError.TOKEN_WRONG_PURPOSE);
        }

        String email = claims.get(EMAIL_CLAIM, String.class);
        if (email == null) {
            throw new DomainException(AuthError.TOKEN_MALFORMED);
        }

        String subject = claims.getSubject();
        if (subject == null) {
            throw new DomainException(AuthError.TOKEN_MALFORMED);
        }

        try {
            return new VerificationToken(UUID.fromString(subject), email, expectedPurpose);
        } catch (IllegalArgumentException e) {
            log.warn("Token subject is not a UUID");
            throw new DomainException(AuthError.TOKEN_MALFORMED);
        }
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new DomainException(AuthError.TOKEN_EXPIRED);
        } catch (JwtException e) {
            log.warn("Rejected verification token: {}", e.getClass().getSimpleName());
            throw new DomainException(AuthError.TOKEN_INVALID);
        } catch (IllegalArgumentException e) {
            throw new DomainException(AuthError.TOKEN_MALFORMED);
        }
    }

    private Duration lifetimeOf(TokenPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> properties.expiration().emailVerification();
            case PASSWORD_RESET -> properties.expiration().passwordReset();
        };
    }
}
