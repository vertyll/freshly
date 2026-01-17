package com.vertyll.freshly.auth.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
class KeycloakConfig {

    private static final String REALM_MASTER = "master";

    @Bean
    public Keycloak keycloak(KeycloakProperties properties) {
        return KeycloakBuilder.builder()
                .serverUrl(properties.serverUrl())
                .realm(REALM_MASTER)
                .clientId(properties.adminClientId())
                .username(properties.adminUsername())
                .password(properties.adminPassword())
                .build();
    }

    @Bean
    public RestClient keycloakRestClient() {
        return RestClient.builder().build();
    }
}
