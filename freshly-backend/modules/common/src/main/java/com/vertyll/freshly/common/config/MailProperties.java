package com.vertyll.freshly.common.config;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "spring.mail")
public record MailProperties(
        String host,
        @Nullable Integer port,
        String username,
        String password,
        String from,
        @Nullable Map<String, String> properties) {}
