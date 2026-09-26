package com.vertyll.freshly.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.auth.application.port.inbound.command.CredentialsUseCase;
import com.vertyll.freshly.auth.application.port.inbound.command.RegistrationUseCase;
import com.vertyll.freshly.auth.application.port.inbound.command.SessionUseCase;
import com.vertyll.freshly.auth.application.port.outbound.IdentityProviderPort;
import com.vertyll.freshly.auth.application.port.outbound.TokenIssuerPort;
import com.vertyll.freshly.auth.application.port.outbound.UserNotificationPort;
import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.auth.application.port.outbound.VerificationLinkFactory;
import com.vertyll.freshly.auth.application.port.outbound.VerificationTokenPort;
import com.vertyll.freshly.auth.application.service.command.CredentialsService;
import com.vertyll.freshly.auth.application.service.command.RegistrationService;
import com.vertyll.freshly.auth.application.service.command.SessionService;
import com.vertyll.freshly.infra.logging.Slf4jUseCaseLogger;

@Configuration("authApplicationBeansConfig")
public class ApplicationBeansConfig {
    @Bean
    @SuppressWarnings("java:S107")
    RegistrationUseCase registrationUseCase(
        IdentityProviderPort identityProvider,
        UserProvisioningPort provisioning,
        UserNotificationPort notifications,
        VerificationTokenPort tokens,
        VerificationLinkFactory links
    ) {
        return new RegistrationService(
            identityProvider,
            provisioning,
            notifications,
            tokens,
            links,
            new Slf4jUseCaseLogger(RegistrationService.class)
        );
    }

    @Bean
    SessionUseCase sessionUseCase(TokenIssuerPort tokenIssuer) {
        return new SessionService(tokenIssuer, new Slf4jUseCaseLogger(SessionService.class));
    }

    @Bean
    CredentialsUseCase credentialsUseCase(
        IdentityProviderPort identityProvider,
        UserProvisioningPort provisioning,
        UserNotificationPort notifications,
        VerificationTokenPort tokens,
        VerificationLinkFactory links
    ) {
        return new CredentialsService(
            identityProvider,
            provisioning,
            notifications,
            tokens,
            links,
            new Slf4jUseCaseLogger(CredentialsService.class)
        );
    }
}
