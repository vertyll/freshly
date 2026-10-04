package com.vertyll.freshly.auth.infrastructure.keycloak;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.auth.domain.model.AuthSession;
import com.vertyll.freshly.auth.infrastructure.config.AuthProperties;
import com.vertyll.freshly.auth.infrastructure.config.KeycloakProperties;
import com.vertyll.freshly.lang.error.DomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakTokenIssuerAdapterTest {
    private static final String TOKEN_URL = "http://keycloak.test/realms/freshly/protocol/openid-connect/token";
    private static final String SUBJECT = "7d1c9a52-3a43-4c34-9a8f-2f5d7c6f1b10";
    private static final String TOKENS = """
            {"access_token":"access-2","refresh_token":"refresh-2"}
            """;

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer keycloak = MockRestServiceServer.bindTo(builder).build();
    private final KeycloakTokenIssuerAdapter adapter = new KeycloakTokenIssuerAdapter(
        builder.build(),
        new KeycloakProperties("http://keycloak.test/realms/freshly", "freshly-app-client", "secret"),
        new AuthProperties("http://app.test/auth/callback", "http://app.test/"),
        token -> Jwt.withTokenValue(token)
            .header("alg", "RS256")
            .subject(SUBJECT)
            .claim("email", "ada@freshly.local")
            .claim("realm_access", Map.of("roles", List.of("USER", "offline_access")))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build(),
        SharedRefreshes.inProcessOnly()
    );

    @Test
    void concurrentRefreshesOfOneTokenReachKeycloakOnce() throws InterruptedException, ExecutionException, TimeoutException {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        keycloak.expect(once(), requestTo(TOKEN_URL)).andRespond(request -> {
            entered.countDown();
            awaitQuietly(release);
            return withSuccess(TOKENS, MediaType.APPLICATION_JSON).createResponse(request);
        });

        CompletableFuture<AuthSession> first = CompletableFuture.supplyAsync(() -> adapter.refresh("refresh-1"));
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<AuthSession> second = CompletableFuture.supplyAsync(() -> adapter.refresh("refresh-1"));
        release.countDown();

        assertThat(first.get(5, TimeUnit.SECONDS).refreshToken()).isEqualTo("refresh-2");
        assertThat(second.get(5, TimeUnit.SECONDS).refreshToken()).isEqualTo("refresh-2");
        assertThat(first.get().subject()).isEqualTo(UUID.fromString(SUBJECT));
        assertThat(first.get().roles()).containsExactly("USER");
        keycloak.verify();
    }

    @Test
    void aSessionReadBeforeTheRefreshGetsTheTokensAlreadyIssued() {
        keycloak.expect(once(), requestTo(TOKEN_URL)).andRespond(withSuccess(TOKENS, MediaType.APPLICATION_JSON));

        adapter.refresh("refresh-1");
        AuthSession stale = adapter.refresh("refresh-1");

        assertThat(stale.accessToken()).isEqualTo("access-2");
        keycloak.verify();
    }

    @Test
    void aRefusedRefreshEndsTheSessionAndIsNotRemembered() {
        keycloak.expect(times(2), requestTo(TOKEN_URL)).andRespond(withBadRequest());

        assertThatThrownBy(() -> adapter.refresh("refresh-1")).isInstanceOfSatisfying(
            DomainException.class,
            e -> assertThat(e.error()).isEqualTo(AuthError.SESSION_EXPIRED)
        );
        assertThatThrownBy(() -> adapter.refresh("refresh-1")).isInstanceOf(DomainException.class);
        keycloak.verify();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
