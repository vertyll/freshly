package com.vertyll.freshly.auth.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@ComponentScan(basePackages = "com.vertyll.freshly.auth.infrastructure")
@EnableConfigurationProperties(
    {
        KeycloakProperties.class,
        JwtProperties.class,
        FrontendProperties.class
    }
)
@EnableAsync
public class AuthModuleConfig {
}
