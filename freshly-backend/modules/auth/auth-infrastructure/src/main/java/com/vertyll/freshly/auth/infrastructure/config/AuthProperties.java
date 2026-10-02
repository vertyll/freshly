package com.vertyll.freshly.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "application.auth")
public record AuthProperties(String callbackUrl, String postLoginUrl) {

    public AuthProperties {
        requireNonNull(callbackUrl, "application.auth.callback-url must be configured");
        requireNonNull(postLoginUrl, "application.auth.post-login-url must be configured");
    }
}
