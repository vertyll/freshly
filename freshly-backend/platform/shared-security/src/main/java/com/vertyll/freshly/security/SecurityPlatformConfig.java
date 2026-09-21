package com.vertyll.freshly.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The platform's security contribution, imported once by {@code bootstrap}.
 *
 * <p>
 * The resource-server configuration, CORS, the JWT role converter and the argument
 * resolvers live here. {@code TestSecurityConfig} deliberately does not: a configuration
 * written to relax security belongs in a test source set, never in the production jar.
 */
@Configuration
@ComponentScan(basePackages = "com.vertyll.freshly.security")
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityPlatformConfig {
}
