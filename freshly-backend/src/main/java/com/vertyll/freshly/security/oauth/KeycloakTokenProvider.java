package com.vertyll.freshly.security.oauth;

import com.vertyll.freshly.security.oauth.exception.KeycloakConfigurationException;
import com.vertyll.freshly.security.oauth.exception.KeycloakTokenException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class KeycloakTokenProvider {

    private final RestClient keycloakClient;
    private final OauthProperties oauthProperties;

    public KeycloakTokenProvider(RestClient keycloakClient, OauthProperties oauthProperties) {
        this.keycloakClient = keycloakClient;
        this.oauthProperties = oauthProperties;
    }

    public String getAccessToken() {
        var registration = oauthProperties.getClient()
                .getRegistration()
                .get(oauthProperties.getRegistrationName());

        if (registration == null) {
            throw new KeycloakConfigurationException(
                    "OAuth2 registration not found: " + oauthProperties.getRegistrationName()
            );
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", registration.clientId());
        formData.add("client_secret", registration.clientSecret());

        try {
            Map<String, Object> response = keycloakClient.post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response != null && response.containsKey("access_token")) {
                return (String) response.get("access_token");
            }

            throw new KeycloakTokenException(
                    "Access token not found in Keycloak response"
            );

        } catch (RestClientException ex) {
            throw new KeycloakTokenException(
                    "Failed to retrieve access token from Keycloak", ex
            );
        }
    }
}
