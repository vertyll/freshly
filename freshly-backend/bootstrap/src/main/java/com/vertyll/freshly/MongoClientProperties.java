package com.vertyll.freshly;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "application.mongodb")
record MongoClientProperties(Duration connectTimeout, Duration readTimeout, Duration serverSelectionTimeout) {

    MongoClientProperties {
        requireNonNull(connectTimeout, "application.mongodb.connect-timeout must be configured");
        requireNonNull(readTimeout, "application.mongodb.read-timeout must be configured");
        requireNonNull(serverSelectionTimeout, "application.mongodb.server-selection-timeout must be configured");
    }
}
