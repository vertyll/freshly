package com.vertyll.freshly.auth.infrastructure.keycloak;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.vertyll.freshly.auth.application.port.outbound.SessionRevocationPort;
import com.vertyll.freshly.auth.infrastructure.config.KeycloakProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KeycloakSessionRevocationAdapter implements SessionRevocationPort {
    private final KeycloakProperties keycloak;
    private final RestClient restClient;

    public KeycloakSessionRevocationAdapter(
        KeycloakProperties keycloak,
        @Qualifier(KeycloakConfig.KEYCLOAK_REST_CLIENT) RestClient restClient
    ) {
        this.keycloak = keycloak;
        this.restClient = restClient;
    }

    @Override
    public void revoke(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloak.clientId());
        form.add("client_secret", keycloak.clientSecret());
        form.add("refresh_token", refreshToken);
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
}
