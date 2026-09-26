package com.vertyll.freshly.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "application.keycloak")
public record KeycloakProperties(
    String serverUrl,
    String realm,
    String adminClientId,
    String adminClientSecret,
    String userClientId,
    String userClientSecret
) {

    public KeycloakProperties {
        requireNonNull(serverUrl, "application.keycloak.server-url must be configured");
        requireNonNull(realm, "application.keycloak.realm must be configured");
        requireNonNull(adminClientId, "application.keycloak.admin-client-id must be configured");
        requireNonNull(adminClientSecret, "application.keycloak.admin-client-secret must be configured");
        requireNonNull(userClientId, "application.keycloak.user-client-id must be configured");
        requireNonNull(userClientSecret, "application.keycloak.user-client-secret must be configured");
    }

    @Override
    public String toString() {
        return "KeycloakProperties[serverUrl=" + serverUrl + ", realm=" + realm + ", adminClientId=" + adminClientId
                + ", adminClientSecret=***, userClientId=" + userClientId + ", userClientSecret=***]";
    }
}
