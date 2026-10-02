package com.vertyll.freshly.auth.infrastructure.web.session;

import java.time.Clock;

import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.auth.application.port.inbound.command.SessionUseCase;

@Configuration
public class SessionRelayConfig {
    @Bean
    FilterRegistrationBean<SessionTokenRelayFilter> sessionTokenRelayFilter(
        BrowserSessions browserSessions,
        SessionUseCase sessions
    ) {
        FilterRegistrationBean<SessionTokenRelayFilter> registration =
                new FilterRegistrationBean<>(new SessionTokenRelayFilter(browserSessions, sessions, Clock.systemUTC()));
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1);
        return registration;
    }
}
