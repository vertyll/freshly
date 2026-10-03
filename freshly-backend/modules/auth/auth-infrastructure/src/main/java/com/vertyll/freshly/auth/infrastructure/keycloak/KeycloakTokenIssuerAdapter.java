package com.vertyll.freshly.auth.infrastructure.keycloak;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.vertyll.freshly.auth.application.port.outbound.TokenIssuerPort;
import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.auth.domain.model.AuthSession;
import com.vertyll.freshly.auth.infrastructure.config.AuthProperties;
import com.vertyll.freshly.auth.infrastructure.config.KeycloakProperties;
import com.vertyll.freshly.lang.error.DomainException;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class KeycloakTokenIssuerAdapter implements TokenIssuerPort {
    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String AUTHORIZATION_CODE = "authorization_code";
    private static final String CODE = "code";
    private static final String CODE_VERIFIER = "code_verifier";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String REFRESH_TOKEN = "refresh_token";

    private static final String EMAIL_CLAIM = "email";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_KEY = "roles";
    private static final String DEFAULT_ROLES_PREFIX = "default-roles-";
    private static final Set<String> BUILT_IN_ROLES = Set.of("offline_access", "uma_authorization");
    private static final Duration REUSE_WINDOW = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final KeycloakProperties keycloak;
    private final AuthProperties auth;
    private final JwtDecoder jwtDecoder;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, Refresh> refreshes = new ConcurrentHashMap<>();

    public KeycloakTokenIssuerAdapter(
        @Qualifier(KeycloakConfig.KEYCLOAK_REST_CLIENT) RestClient keycloakRestClient,
        KeycloakProperties keycloak,
        AuthProperties auth,
        JwtDecoder jwtDecoder
    ) {
        this.restClient = keycloakRestClient;
        this.keycloak = keycloak;
        this.auth = auth;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public AuthSession exchange(String code, String codeVerifier) {
        MultiValueMap<String, String> form = clientForm();
        form.add(GRANT_TYPE, AUTHORIZATION_CODE);
        form.add(CODE, code);
        form.add(CODE_VERIFIER, codeVerifier);
        form.add(REDIRECT_URI, auth.callbackUrl());

        return post(form, AuthError.SIGN_IN_REJECTED);
    }

    @Override
    public AuthSession refresh(String refreshToken) {
        forgetOldRefreshes();
        Refresh mine = new Refresh();
        Refresh running = refreshes.putIfAbsent(refreshToken, mine);
        if (running != null) {
            Outcome outcome = running.await();
            AuthSession shared = outcome.session();
            if (shared != null) {
                return shared;
            }
            DomainException failure = outcome.failure();
            if (failure != null) {
                throw new DomainException(failure.error(), Map.of(), failure);
            }
            return requestRefresh(refreshToken);
        }

        try {
            AuthSession session = requestRefresh(refreshToken);
            mine.finish(new Outcome(session, null, clock.instant()));
            return session;
        } catch (DomainException e) {
            refreshes.remove(refreshToken, mine);
            mine.finish(new Outcome(null, e, clock.instant()));
            throw e;
        } finally {
            if (!mine.isFinished()) {
                refreshes.remove(refreshToken, mine);
                mine.finish(new Outcome(null, null, clock.instant()));
            }
        }
    }

    private AuthSession requestRefresh(String refreshToken) {
        MultiValueMap<String, String> form = clientForm();
        form.add(GRANT_TYPE, REFRESH_TOKEN);
        form.add(REFRESH_TOKEN, refreshToken);

        return post(form, AuthError.SESSION_EXPIRED);
    }

    private void forgetOldRefreshes() {
        Instant oldest = clock.instant().minus(REUSE_WINDOW);
        refreshes.values().removeIf(refresh -> refresh.finishedBefore(oldest));
    }

    @Override
    public void revoke(String refreshToken) {
        MultiValueMap<String, String> form = clientForm();
        form.add(REFRESH_TOKEN, refreshToken);

        try {
            restClient.post()
                .uri(keycloak.endpoint("logout"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Keycloak did not end the session: {}", e.getMessage());
        }
    }

    private MultiValueMap<String, String> clientForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(CLIENT_ID, keycloak.userClientId());
        form.add(CLIENT_SECRET, keycloak.userClientSecret());
        return form;
    }

    private AuthSession post(MultiValueMap<String, String> form, AuthError onRejection) {
        KeycloakTokenResponse response;
        try {
            response = restClient.post()
                .uri(keycloak.endpoint("token"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(form)
                .retrieve()
                .body(KeycloakTokenResponse.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new DomainException(onRejection, Map.of(), e);
            }
            log.error("Keycloak token endpoint failed with {}", e.getStatusCode(), e);
            throw new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE, Map.of(), e);
        } catch (RestClientException e) {
            throw new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE, Map.of(), e);
        }

        if (response == null || response.refreshToken() == null) {
            throw new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE);
        }
        return toSession(response.accessToken(), response.refreshToken(), onRejection);
    }

    private AuthSession toSession(String accessToken, String refreshToken, AuthError onRejection) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(accessToken);
        } catch (JwtException e) {
            throw new DomainException(onRejection, Map.of(), e);
        }

        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString(EMAIL_CLAIM);
        Instant expiresAt = jwt.getExpiresAt();
        if (subject == null || email == null || expiresAt == null) {
            throw new DomainException(onRejection);
        }
        return new AuthSession(UUID.fromString(subject), email, roles(jwt), accessToken, refreshToken, expiresAt);
    }

    private static Set<String> roles(Jwt jwt) {
        if (!(jwt.getClaims().get(REALM_ACCESS_CLAIM) instanceof Map<?, ?> realmAccess)
                || !(realmAccess.get(ROLES_KEY) instanceof Collection<?> roles)) {
            return Set.of();
        }
        return roles.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter(role -> !BUILT_IN_ROLES.contains(role) && !role.startsWith(DEFAULT_ROLES_PREFIX))
            .collect(Collectors.toUnmodifiableSet());
    }

    private record Outcome(@Nullable AuthSession session, @Nullable DomainException failure, Instant at) {
    }

    private static final class Refresh {
        private final CompletableFuture<Outcome> result = new CompletableFuture<>();

        void finish(Outcome outcome) {
            result.complete(outcome);
        }

        boolean isFinished() {
            return result.isDone();
        }

        Outcome await() {
            return result.join();
        }

        boolean finishedBefore(Instant instant) {
            Outcome outcome = result.getNow(null);
            return outcome != null && outcome.at().isBefore(instant);
        }
    }

    private record KeycloakTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") @Nullable String refreshToken
    ) {
    }
}
