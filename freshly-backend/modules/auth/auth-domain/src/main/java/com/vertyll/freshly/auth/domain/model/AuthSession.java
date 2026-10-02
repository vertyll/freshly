package com.vertyll.freshly.auth.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record AuthSession(
    UUID subject,
    String email,
    Set<String> roles,
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt
) {

    public AuthSession {
        requireNonNull(subject, "subject");
        requireNonNull(email, "email");
        requireNonNull(accessToken, "accessToken");
        requireNonNull(refreshToken, "refreshToken");
        requireNonNull(accessTokenExpiresAt, "accessTokenExpiresAt");
        roles = Set.copyOf(roles);
    }

    public boolean needsRefreshAt(Instant now, Duration skew) {
        return !now.plus(skew).isBefore(accessTokenExpiresAt);
    }

    @Override
    public String toString() {
        return "AuthSession[subject=" + subject + ", email=" + email + ", roles=" + roles
                + ", accessToken=***, refreshToken=***, accessTokenExpiresAt=" + accessTokenExpiresAt + "]";
    }
}
