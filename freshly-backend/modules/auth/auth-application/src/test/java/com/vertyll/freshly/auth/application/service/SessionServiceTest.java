package com.vertyll.freshly.auth.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.auth.application.service.command.SessionService;
import com.vertyll.freshly.auth.domain.model.SignedInUser;
import com.vertyll.freshly.lang.logging.RecordingUseCaseLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionServiceTest {
    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final SignedInUser USER = new SignedInUser(SUBJECT, "ada@freshly.local", Set.of("USER"));
    private static final String REFRESH_TOKEN = "refresh";

    private final List<String> revoked = new ArrayList<>();
    private final Map<UUID, Set<String>> provisioned = new LinkedHashMap<>();

    @Test
    @DisplayName("signing in provisions the account with the roles the token carries")
    void signInProvisions() {
        SessionService service = new SessionService(revoked::add, provisioned::put, new RecordingUseCaseLogger());

        service.signIn(USER, REFRESH_TOKEN);

        assertThat(provisioned).containsEntry(SUBJECT, Set.of("USER"));
        assertThat(revoked).isEmpty();
    }

    @Test
    @DisplayName("a failed provisioning revokes the session Keycloak already opened")
    void failedProvisioningRevokes() {
        UserProvisioningPort failing = (userId, roles) -> {
            throw new IllegalStateException("database down");
        };
        SessionService service = new SessionService(revoked::add, failing, new RecordingUseCaseLogger());

        assertThatThrownBy(() -> service.signIn(USER, REFRESH_TOKEN)).hasMessage("database down");

        assertThat(revoked).containsExactly(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("signing out revokes the refresh token")
    void signOutRevokes() {
        new SessionService(revoked::add, provisioned::put, new RecordingUseCaseLogger()).signOut(REFRESH_TOKEN);

        assertThat(revoked).containsExactly(REFRESH_TOKEN);
    }
}
