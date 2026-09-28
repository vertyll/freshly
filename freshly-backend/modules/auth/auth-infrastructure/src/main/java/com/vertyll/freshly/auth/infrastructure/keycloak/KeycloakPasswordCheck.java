package com.vertyll.freshly.auth.infrastructure.keycloak;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.auth.infrastructure.config.KeycloakProperties;
import com.vertyll.freshly.lang.error.DomainException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
final class KeycloakPasswordCheck {
    @SuppressWarnings("java:S1075")
    private static final String TOKEN_PATH = "/realms/{realm}/protocol/openid-connect/token";

    private KeycloakPasswordCheck() {
    }

    static boolean succeeds(RestClient restClient, KeycloakProperties properties, String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", properties.userClientId());
        form.add("client_secret", properties.userClientSecret());
        form.add("username", username);
        form.add("password", password);

        try {
            restClient.post()
                .uri(TOKEN_PATH, Map.of("realm", properties.realm()))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(form)
                .retrieve()
                .toBodilessEntity();
            return true;

        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.debug("Password verification rejected by Keycloak");
                return false;
            }
            throw new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE, Map.of(), e);
        } catch (RestClientException e) {
            throw new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE, Map.of(), e);
        }
    }
}
