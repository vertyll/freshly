package com.vertyll.freshly.common.config;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "application")
@Validated
public record ApplicationProperties(Frontend frontend) {
    public record Frontend(@NotBlank(message = "Frontend URL is required") String url) {
    }
}
