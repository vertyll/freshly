package com.vertyll.freshly.translation.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@ComponentScan(basePackages = "com.vertyll.freshly.translation.infrastructure")
@EnableMongoRepositories(basePackages = "com.vertyll.freshly.translation.infrastructure.persistence.repository")
public class TranslationModuleConfig {
}
