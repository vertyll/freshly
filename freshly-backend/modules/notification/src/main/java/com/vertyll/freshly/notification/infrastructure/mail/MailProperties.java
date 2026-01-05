package com.vertyll.freshly.notification.infrastructure.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.mail")
@Validated
record MailProperties(String host, int port, String username, String password, String from) {}
