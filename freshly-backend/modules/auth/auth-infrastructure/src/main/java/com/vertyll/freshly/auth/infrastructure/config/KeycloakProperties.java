package com.vertyll.freshly.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "application.keycloak")
public record KeycloakProperties(String realmUrl, String clientId, String clientSecret) {

    public KeycloakProperties {
        requireNonNull(realmUrl, "application.keycloak.realm-url must be configured");
        requireNonNull(clientId, "application.keycloak.client-id must be configured");
        requireNonNull(clientSecret, "application.keycloak.client-secret must be configured");
    }

    public String endpoint(String name) {
        return realmUrl + "/protocol/openid-connect/" + name;
    }

    @Override
    public String toString() {
        return "KeycloakProperties[realmUrl=" + realmUrl + ", clientId=" + clientId + ", clientSecret=***]";
    }
}
