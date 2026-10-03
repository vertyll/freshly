package com.vertyll.freshly.useraccess.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "application.keycloak")
public record KeycloakRealmProperties(String realmUrl, Admin admin) {
    private static final String REALMS = "/realms/";

    public KeycloakRealmProperties {
        requireNonNull(realmUrl, "application.keycloak.realm-url must be configured");
        requireNonNull(admin, "application.keycloak.admin must be configured");
    }

    public String serverUrl() {
        return realmUrl.substring(0, realmUrl.indexOf(REALMS));
    }

    public String realm() {
        return realmUrl.substring(realmUrl.indexOf(REALMS) + REALMS.length());
    }

    public record Admin(String clientId, String clientSecret) {
        public Admin {
            requireNonNull(clientId, "application.keycloak.admin.client-id must be configured");
            requireNonNull(clientSecret, "application.keycloak.admin.client-secret must be configured");
        }

        @Override
        public String toString() {
            return "Admin[clientId=" + clientId + ", clientSecret=***]";
        }
    }
}
