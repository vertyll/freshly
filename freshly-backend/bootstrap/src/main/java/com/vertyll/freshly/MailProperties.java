package com.vertyll.freshly;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "application.mail")
public record MailProperties(String from) {

    public MailProperties {
        requireNonNull(from, "application.mail.from must be configured");
    }
}
