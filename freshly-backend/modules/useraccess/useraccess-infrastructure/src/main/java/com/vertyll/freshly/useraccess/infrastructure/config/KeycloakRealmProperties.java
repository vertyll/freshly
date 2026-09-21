package com.vertyll.freshly.useraccess.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "application.keycloak")
public record KeycloakRealmProperties(String serverUrl, String realm, String adminClientId, String adminClientSecret) {

    public KeycloakRealmProperties {
        requireNonNull(serverUrl, "application.keycloak.server-url must be configured");
        requireNonNull(realm, "application.keycloak.realm must be configured");
        requireNonNull(adminClientId, "application.keycloak.admin-client-id must be configured");
        requireNonNull(adminClientSecret, "application.keycloak.admin-client-secret must be configured");
    }

    @Override
    public String toString() {
        return "KeycloakRealmProperties[serverUrl=" + serverUrl + ", realm=" + realm + ", adminClientId="
                + adminClientId + ", adminClientSecret=***]";
    }
}
