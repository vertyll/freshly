package com.vertyll.freshly.airquality.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "application.airquality.gios")
public record GiosProperties(String baseUrl, Duration connectTimeout, Duration readTimeout, String sslBundle) {
    public GiosProperties {
        requireNonNull(baseUrl, "application.airquality.gios.base-url must be configured");
        requireNonNull(connectTimeout, "application.airquality.gios.connect-timeout must be configured");
        requireNonNull(readTimeout, "application.airquality.gios.read-timeout must be configured");
        requireNonNull(sslBundle, "application.airquality.gios.ssl-bundle must be configured");
    }
}
