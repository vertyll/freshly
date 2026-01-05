package com.vertyll.freshly.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "external-service")
@Validated
public record ExternalServiceProperties(Gios gios) {
    public record Gios(String apiUrl) {}
}
