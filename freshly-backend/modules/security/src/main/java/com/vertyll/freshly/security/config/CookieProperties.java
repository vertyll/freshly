package com.vertyll.freshly.security.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "security.cookie")
@Validated
public record CookieProperties(Boolean httpOnly, Boolean secure, String sameSite, String path) {
    private static final String DEFAULT_SAMESITE = "Strict";
    private static final String DEFAULT_PATH = "/";
    private static final String SAMESITE_LAX = "Lax";
    private static final String SAMESITE_NONE = "None";

    private static final String INVALID_SAMESITE = "Invalid SameSite value: ";

    public CookieProperties {
        if (httpOnly == null) httpOnly = true;
        if (secure == null) secure = true;
        if (sameSite == null) sameSite = DEFAULT_SAMESITE;
        if (path == null) path = DEFAULT_PATH;

        List<String> validSameSite = List.of(DEFAULT_SAMESITE, SAMESITE_LAX, SAMESITE_NONE);
        if (!validSameSite.contains(sameSite)) {
            throw new IllegalArgumentException(INVALID_SAMESITE + sameSite);
        }
    }
}
