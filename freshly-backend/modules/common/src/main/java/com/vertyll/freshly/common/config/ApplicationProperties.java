package com.vertyll.freshly.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "application")
@Validated
public record ApplicationProperties(Frontend frontend) {
    public record Frontend(@NotBlank(message = "Frontend URL is required") String url) {}
}
