package com.vertyll.freshly.auth.infrastructure.keycloak;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.vertyll.freshly.auth.infrastructure.config.KeycloakProperties;

@Configuration
public class KeycloakConfig {
    public static final String AUTH_KEYCLOAK = "authKeycloakClient";
    public static final String KEYCLOAK_REST_CLIENT = "keycloakRestClient";

    @Bean(name = AUTH_KEYCLOAK, destroyMethod = "close")
    Keycloak keycloakAdminClient(KeycloakProperties properties) {
        return KeycloakBuilder.builder()
            .serverUrl(properties.serverUrl())
            .realm(properties.realm())
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId(properties.adminClientId())
            .clientSecret(properties.adminClientSecret())
            .build();
    }

    @Bean(KEYCLOAK_REST_CLIENT)
    RestClient keycloakRestClient(KeycloakProperties properties) {
        return RestClient.builder().baseUrl(properties.serverUrl()).build();
    }
}
