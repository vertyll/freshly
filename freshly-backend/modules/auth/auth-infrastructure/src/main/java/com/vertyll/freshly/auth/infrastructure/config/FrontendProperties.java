package com.vertyll.freshly.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.frontend")
public record FrontendProperties(String url) {
}
