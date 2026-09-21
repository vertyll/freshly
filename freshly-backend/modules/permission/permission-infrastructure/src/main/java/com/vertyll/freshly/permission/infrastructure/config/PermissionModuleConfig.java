package com.vertyll.freshly.permission.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@ComponentScan(basePackages = "com.vertyll.freshly.permission.infrastructure")
@EnableMongoRepositories(basePackages = "com.vertyll.freshly.permission.infrastructure.persistence.repository")
public class PermissionModuleConfig {
}
