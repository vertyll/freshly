package com.vertyll.freshly.notification.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.vertyll.freshly.notification.infrastructure")
@EnableConfigurationProperties(MailProperties.class)
public class NotificationModuleConfig {
}
