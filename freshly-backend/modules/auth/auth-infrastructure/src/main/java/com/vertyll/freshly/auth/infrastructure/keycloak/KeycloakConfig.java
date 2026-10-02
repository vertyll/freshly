package com.vertyll.freshly.auth.infrastructure.keycloak;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class KeycloakConfig {
    public static final String KEYCLOAK_REST_CLIENT = "keycloakRestClient";

    @Bean(KEYCLOAK_REST_CLIENT)
    RestClient keycloakRestClient() {
        return RestClient.create();
    }
}
