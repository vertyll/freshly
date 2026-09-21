package com.vertyll.freshly.useraccess.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.infra.logging.Slf4jUseCaseLogger;
import com.vertyll.freshly.infra.transaction.TransactionalUseCaseFactory;
import com.vertyll.freshly.useraccess.application.port.inbound.command.UserAccessCommandUseCase;
import com.vertyll.freshly.useraccess.application.port.inbound.query.RoleDirectoryQueryUseCase;
import com.vertyll.freshly.useraccess.application.port.inbound.query.UserAccessQueryUseCase;
import com.vertyll.freshly.useraccess.application.port.outbound.RoleDirectoryPort;
import com.vertyll.freshly.useraccess.application.service.command.UserAccessCommandService;
import com.vertyll.freshly.useraccess.application.service.query.RoleDirectoryQueryService;
import com.vertyll.freshly.useraccess.application.service.query.UserAccessQueryService;
import com.vertyll.freshly.useraccess.domain.repository.SystemUserRepository;

@Configuration
public class ApplicationBeansConfig {
    @Bean
    UserAccessCommandUseCase userAccessCommandUseCase(
        TransactionalUseCaseFactory transactions,
        SystemUserRepository users,
        RoleDirectoryPort directory
    ) {
        return transactions.readWrite(
            UserAccessCommandUseCase.class,
            new UserAccessCommandService(users, directory, new Slf4jUseCaseLogger(UserAccessCommandService.class))
        );
    }

    @Bean
    UserAccessQueryUseCase userAccessQueryUseCase(
        TransactionalUseCaseFactory transactions,
        SystemUserRepository users
    ) {
        return transactions.readOnly(UserAccessQueryUseCase.class, new UserAccessQueryService(users));
    }

    @Bean
    RoleDirectoryQueryUseCase roleDirectoryQueryUseCase(RoleDirectoryPort directory) {
        return new RoleDirectoryQueryService(directory);
    }
}
