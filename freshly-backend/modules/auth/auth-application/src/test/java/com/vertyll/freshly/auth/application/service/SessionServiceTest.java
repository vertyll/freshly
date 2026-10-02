package com.vertyll.freshly.auth.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.auth.application.port.outbound.TokenIssuerPort;
import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.auth.application.service.command.SessionService;
import com.vertyll.freshly.auth.domain.model.AuthSession;
import com.vertyll.freshly.lang.logging.RecordingUseCaseLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionServiceTest {
    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final AuthSession SESSION =
            new AuthSession(SUBJECT, "ada@freshly.local", Set.of("USER"), "access", "refresh", Instant.EPOCH);

    private final FakeTokenIssuer tokens = new FakeTokenIssuer();
    private final Map<UUID, Set<String>> provisioned = new LinkedHashMap<>();

    @Test
    @DisplayName("signing in provisions the account with the roles the token carries")
    void signInProvisions() {
        SessionService service = new SessionService(tokens, provisioned::put, new RecordingUseCaseLogger());

        AuthSession session = service.signIn("code", "verifier");

        assertThat(session).isEqualTo(SESSION);
        assertThat(provisioned).containsEntry(SUBJECT, Set.of("USER"));
        assertThat(tokens.revoked).isEmpty();
    }

    @Test
    @DisplayName("a failed provisioning revokes the session Keycloak already opened")
    void failedProvisioningRevokes() {
        UserProvisioningPort failing = (userId, roles) -> {
            throw new IllegalStateException("database down");
        };
        SessionService service = new SessionService(tokens, failing, new RecordingUseCaseLogger());

        assertThatThrownBy(() -> service.signIn("code", "verifier")).hasMessage("database down");

        assertThat(tokens.revoked).containsExactly("refresh");
    }

    @Test
    @DisplayName("signing out revokes the refresh token")
    void signOutRevokes() {
        new SessionService(tokens, provisioned::put, new RecordingUseCaseLogger()).signOut(SESSION);

        assertThat(tokens.revoked).containsExactly("refresh");
    }

    private static final class FakeTokenIssuer implements TokenIssuerPort {
        private final List<String> revoked = new ArrayList<>();

        @Override
        public AuthSession exchange(String code, String codeVerifier) {
            return SESSION;
        }

        @Override
        public AuthSession refresh(String refreshToken) {
            return SESSION;
        }

        @Override
        public void revoke(String refreshToken) {
            revoked.add(refreshToken);
        }
    }
}
