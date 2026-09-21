package com.vertyll.freshly.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.mail")
public record MailProperties(String from) {

    private static final String FROM_REQUIRED = "application.mail.from must be configured";

    public MailProperties {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException(FROM_REQUIRED);
        }
    }
}
