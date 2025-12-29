package com.vertyll.freshly.security.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "application.keycloak")
@Validated
public record KeycloakProperties (
        String serverUrl,
        String realm,
        String adminClientId,
        String adminUsername,
        String adminPassword,
        String userClientId
) {
}
