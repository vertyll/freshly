package com.vertyll.freshly.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Map;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    public JavaMailSender getJavaMailSender(MailProperties mailProperties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(mailProperties.host());
        Integer port = mailProperties.port();
        if (port != null) {
            mailSender.setPort(port);
        }
        mailSender.setUsername(mailProperties.username());
        mailSender.setPassword(mailProperties.password());
        mailSender.setDefaultEncoding("UTF-8");

        Map<String, String> properties = mailProperties.properties();
        if (properties != null && !properties.isEmpty()) {
            Properties props = mailSender.getJavaMailProperties();
            props.putAll(properties);
        }

        return mailSender;
    }
}
