package com.vertyll.freshly.useraccess.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@ComponentScan(basePackages = "com.vertyll.freshly.useraccess.infrastructure")
@EnableMongoRepositories(basePackages = "com.vertyll.freshly.useraccess.infrastructure.persistence.repository")
@EnableConfigurationProperties(KeycloakRealmProperties.class)
public class UserAccessModuleConfig {
}
