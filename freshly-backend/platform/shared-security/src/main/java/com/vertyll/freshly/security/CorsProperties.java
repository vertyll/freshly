package com.vertyll.freshly.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which origins the browser API accepts.
 *
 * <p>
 * A list rather than a single value, and with no default: an origin list that falls
 * back to {@code *} when unconfigured is the kind of default that survives into
 * production. Boot fails to start if it is missing, which is the right moment to find
 * out.
 */
@ConfigurationProperties(prefix = "application.cors")
public record CorsProperties(List<String> allowedOrigins, List<String> allowedMethods) {
    private static final String ORIGINS_REQUIRED = "application.cors.allowed-origins must name at least one origin";

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException(ORIGINS_REQUIRED);
        }
        allowedOrigins = List.copyOf(allowedOrigins);
        allowedMethods = allowedMethods == null || allowedMethods.isEmpty()
                ? List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") : List.copyOf(allowedMethods);
    }
}
