package com.vertyll.freshly.notification.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.infra.logging.Slf4jUseCaseLogger;
import com.vertyll.freshly.notification.application.port.inbound.NotificationCommandUseCase;
import com.vertyll.freshly.notification.application.port.outbound.EmailDispatchPort;
import com.vertyll.freshly.notification.application.service.NotificationCommandService;

@Configuration("notificationApplicationBeansConfig")
public class ApplicationBeansConfig {
    @Bean
    NotificationCommandUseCase notificationCommandUseCase(EmailDispatchPort dispatch) {
        return new NotificationCommandService(dispatch, new Slf4jUseCaseLogger(NotificationCommandService.class));
    }
}
