package com.vertyll.freshly.airquality.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@ComponentScan(basePackages = "com.vertyll.freshly.airquality.infrastructure")
@EnableMongoRepositories(basePackages = "com.vertyll.freshly.airquality.infrastructure.persistence.repository")
@EnableConfigurationProperties(GiosProperties.class)
public class AirQualityModuleConfig {
}
