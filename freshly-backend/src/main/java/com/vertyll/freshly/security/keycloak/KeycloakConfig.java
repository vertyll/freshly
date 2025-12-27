package com.vertyll.freshly.security.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakConfig {

    @Bean
    public Keycloak keycloak(KeycloakProperties properties) {
        return KeycloakBuilder.builder()
                .serverUrl(properties.getServerUrl())
                .realm("master")
                .clientId(properties.getAdminClientId())
                .username(properties.getAdminUsername())
                .password(properties.getAdminPassword())
                .build();
    }

    @Bean
    public RestClient keycloakRestClient() {
        return RestClient.builder()
                .build();
    }
}
