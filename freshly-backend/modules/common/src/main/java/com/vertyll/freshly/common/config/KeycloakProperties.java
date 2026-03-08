package com.vertyll.freshly.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "application.keycloak")
@Validated
public record KeycloakProperties(
        String serverUrl,
        String realm,
        String adminClientId,
        String adminClientSecret,
        String userClientId,
        String userClientSecret,
        String rolesClaimPath,
        CookieProperties cookie) {

    public record CookieProperties(
            String refreshTokenCookieName,
            boolean httpOnly,
            boolean secure,
            String sameSite,
            String path) {}
}
