package com.vertyll.freshly.security.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "security.cookie")
@Validated
public record CookieProperties(Boolean httpOnly, Boolean secure, String sameSite, String path) {
    public CookieProperties {
        if (httpOnly == null) httpOnly = true;
        if (secure == null) secure = true;
        if (sameSite == null) sameSite = "Strict";
        if (path == null) path = "/";

        List<String> validSameSite = List.of("Strict", "Lax", "None");
        if (!validSameSite.contains(sameSite)) {
            throw new IllegalArgumentException("Invalid SameSite value: " + sameSite);
        }
    }
}
