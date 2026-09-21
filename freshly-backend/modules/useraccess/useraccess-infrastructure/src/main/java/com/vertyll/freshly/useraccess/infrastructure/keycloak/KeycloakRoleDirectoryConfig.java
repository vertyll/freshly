package com.vertyll.freshly.useraccess.infrastructure.keycloak;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.useraccess.infrastructure.config.KeycloakRealmProperties;

@Configuration
public class KeycloakRoleDirectoryConfig {
    public static final String USERACCESS_KEYCLOAK = "userAccessKeycloakClient";

    @Bean(name = USERACCESS_KEYCLOAK, destroyMethod = "close")
    Keycloak userAccessKeycloakClient(KeycloakRealmProperties properties) {
        return KeycloakBuilder.builder()
            .serverUrl(properties.serverUrl())
            .realm(properties.realm())
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId(properties.adminClientId())
            .clientSecret(properties.adminClientSecret())
            .build();
    }
}
