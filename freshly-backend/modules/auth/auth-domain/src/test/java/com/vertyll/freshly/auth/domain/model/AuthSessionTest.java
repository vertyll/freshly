package com.vertyll.freshly.auth.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSessionTest {
    private static final Instant EXPIRY = Instant.parse("2026-10-02T12:00:00Z");
    private static final Duration SKEW = Duration.ofSeconds(30);

    private final AuthSession session =
            new AuthSession(UUID.randomUUID(), "ada@freshly.local", Set.of("USER"), "access", "refresh", EXPIRY);

    @Test
    @DisplayName("a token well before expiry is used as it is")
    void freshTokenNeedsNoRefresh() {
        assertThat(session.needsRefreshAt(EXPIRY.minusSeconds(60), SKEW)).isFalse();
    }

    @Test
    @DisplayName("a token inside the skew is refreshed before it can expire in flight")
    void tokenInsideSkewNeedsRefresh() {
        assertThat(session.needsRefreshAt(EXPIRY.minusSeconds(10), SKEW)).isTrue();
    }

    @Test
    @DisplayName("the tokens never reach a log line")
    void toStringMasksTokens() {
        assertThat(session.toString()).doesNotContain("access,").doesNotContain("refresh,").contains("***");
    }
}
