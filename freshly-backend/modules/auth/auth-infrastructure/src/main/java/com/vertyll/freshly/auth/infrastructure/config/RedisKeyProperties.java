package com.vertyll.freshly.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static java.util.Objects.requireNonNull;

@ConfigurationProperties(prefix = "application.redis")
public record RedisKeyProperties(String keyPrefix) {

    public RedisKeyProperties {
        requireNonNull(keyPrefix, "application.redis.key-prefix must be configured");
    }
}
