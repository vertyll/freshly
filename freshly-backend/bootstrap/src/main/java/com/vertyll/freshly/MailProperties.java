package com.vertyll.freshly;

import static java.util.Objects.requireNonNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.mail")
public record MailProperties(String from) {

    public MailProperties {
        requireNonNull(from, "application.mail.from must be configured");
    }
}
