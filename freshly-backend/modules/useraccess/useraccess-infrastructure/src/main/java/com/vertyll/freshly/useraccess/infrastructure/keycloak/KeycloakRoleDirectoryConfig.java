package com.vertyll.freshly.useraccess.infrastructure.keycloak;

import java.util.concurrent.TimeUnit;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.spi.ResteasyClientClassicProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.useraccess.infrastructure.config.KeycloakRealmProperties;

/**
 * The admin client. Its RESTEasy client is the one Keycloak's builder makes by default —
 * the same pool and the same JSON provider — with the connect and read timeouts of
 * {@code application.keycloak}, which the default leaves unbounded.
 */
@Configuration
public class KeycloakRoleDirectoryConfig {
    public static final String USERACCESS_KEYCLOAK = "userAccessKeycloakClient";

    @Bean(name = USERACCESS_KEYCLOAK, destroyMethod = "close")
    Keycloak userAccessKeycloakClient(KeycloakRealmProperties properties) {
        return KeycloakBuilder.builder()
            .serverUrl(properties.serverUrl())
            .realm(properties.realm())
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId(properties.admin().clientId())
            .clientSecret(properties.admin().clientSecret())
            .resteasyClient(
                ResteasyClientClassicProvider.createClientBuilder()
                    .connectTimeout(properties.connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .readTimeout(properties.readTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .build()
                    .register(JacksonProvider.class, 100)
            )
            .build();
    }
}
