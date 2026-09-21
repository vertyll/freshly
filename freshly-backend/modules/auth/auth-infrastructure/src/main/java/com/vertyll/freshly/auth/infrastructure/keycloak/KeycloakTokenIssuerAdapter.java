package com.vertyll.freshly.auth.infrastructure.keycloak;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.vertyll.freshly.auth.application.dto.AuthTokens;
import com.vertyll.freshly.auth.application.port.outbound.TokenIssuerPort;
import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.auth.infrastructure.config.KeycloakProperties;
import com.vertyll.freshly.lang.error.DomainException;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class KeycloakTokenIssuerAdapter implements TokenIssuerPort {
    private static final String TOKEN_PATH = "/realms/{realm}/protocol/openid-connect/token";
    private static final String LOGOUT_PATH = "/realms/{realm}/protocol/openid-connect/logout";

    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_ID = "client_id";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String REFRESH_TOKEN = "refresh_token";

    private final RestClient restClient;
    private final KeycloakProperties properties;

    public KeycloakTokenIssuerAdapter(
        @Qualifier(KeycloakConfig.KEYCLOAK_REST_CLIENT) RestClient keycloakRestClient,
        KeycloakProperties properties
    ) {
        this.restClient = keycloakRestClient;
        this.properties = properties;
    }

    @Override
    public AuthTokens issue(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(GRANT_TYPE, PASSWORD);
        form.add(CLIENT_ID, properties.publicClientId());
        form.add(USERNAME, username);
        form.add(PASSWORD, password);

        return post(TOKEN_PATH, form, AuthError.INVALID_CREDENTIALS);
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(GRANT_TYPE, REFRESH_TOKEN);
        form.add(CLIENT_ID, properties.publicClientId());
        form.add(REFRESH_TOKEN, refreshToken);

        return post(TOKEN_PATH, form, AuthError.REFRESH_TOKEN_INVALID);
    }

    @Override
    public void revoke(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(CLIENT_ID, properties.publicClientId());
        form.add(REFRESH_TOKEN, refreshToken);

        try {
            restClient.post()
                .uri(LOGOUT_PATH, properties.realm())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Keycloak refused a logout with status {}", e.getStatusCode());
        }
    }

    private AuthTokens post(String path, MultiValueMap<String, String> form, AuthError onRejection) {
        try {
            KeycloakTokenResponse response = restClient.post()
                .uri(path, properties.realm())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(form)
                .retrieve()
                .body(KeycloakTokenResponse.class);

            if (response == null) {
                throw new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE);
            }

            return new AuthTokens(
                response.accessToken(),
                response.refreshToken(),
                response.tokenType(),
                response.expiresIn(),
                response.refreshExpiresIn()
            );

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new DomainException(onRejection);
            }
            log.error("Keycloak token endpoint failed with {}", e.getStatusCode(), e);
            throw new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE);
        }
    }

    private record KeycloakTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") @Nullable String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("refresh_expires_in") long refreshExpiresIn
    ) {
    }
}
