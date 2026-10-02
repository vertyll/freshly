package com.vertyll.freshly.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.auth.application.port.inbound.command.SessionUseCase;
import com.vertyll.freshly.auth.application.port.outbound.TokenIssuerPort;
import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.auth.application.service.command.SessionService;
import com.vertyll.freshly.infra.logging.Slf4jUseCaseLogger;

@Configuration("authApplicationBeansConfig")
public class ApplicationBeansConfig {
    @Bean
    SessionUseCase sessionUseCase(TokenIssuerPort tokenIssuer, UserProvisioningPort provisioning) {
        return new SessionService(tokenIssuer, provisioning, new Slf4jUseCaseLogger(SessionService.class));
    }
}
