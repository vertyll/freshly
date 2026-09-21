package com.vertyll.freshly.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "application.keycloak")
public record KeycloakProperties(
    String serverUrl,
    String realm,
    String adminClientId,
    String adminClientSecret,
    String publicClientId
) {

    public KeycloakProperties {
        requireNonNull(serverUrl, "application.keycloak.server-url must be configured");
        requireNonNull(realm, "application.keycloak.realm must be configured");
        requireNonNull(adminClientId, "application.keycloak.admin-client-id must be configured");
        requireNonNull(adminClientSecret, "application.keycloak.admin-client-secret must be configured");
        requireNonNull(publicClientId, "application.keycloak.public-client-id must be configured");
    }

    @Override
    public String toString() {
        return "KeycloakProperties[serverUrl=" + serverUrl + ", realm=" + realm + ", adminClientId=" + adminClientId
                + ", adminClientSecret=***" + ", publicClientId=" + publicClientId + "]";
    }
}
