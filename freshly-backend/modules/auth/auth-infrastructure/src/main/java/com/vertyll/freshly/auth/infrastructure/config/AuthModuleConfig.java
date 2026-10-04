package com.vertyll.freshly.auth.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.vertyll.freshly.auth.infrastructure")
@EnableConfigurationProperties(
    {
        KeycloakProperties.class,
        AuthProperties.class,
        RedisKeyProperties.class
    }
)
public class AuthModuleConfig {
}
