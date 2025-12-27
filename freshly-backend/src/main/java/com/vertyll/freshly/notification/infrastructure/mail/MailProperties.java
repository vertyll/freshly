package com.vertyll.freshly.notification.infrastructure.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.mail")
class MailProperties {
    private String host;
    private int port;
    private String username;
    private String password;
    private String from;
}
