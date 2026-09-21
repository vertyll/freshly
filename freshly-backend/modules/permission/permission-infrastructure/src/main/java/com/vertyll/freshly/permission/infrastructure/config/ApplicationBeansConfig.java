package com.vertyll.freshly.permission.infrastructure.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.authz.PermissionCatalogue;
import com.vertyll.freshly.infra.logging.Slf4jUseCaseLogger;
import com.vertyll.freshly.infra.transaction.TransactionalUseCaseFactory;
import com.vertyll.freshly.permission.application.port.inbound.command.RoleAuthorityCommandUseCase;
import com.vertyll.freshly.permission.application.port.inbound.query.PermissionQueryUseCase;
import com.vertyll.freshly.permission.application.service.command.RoleAuthorityCommandService;
import com.vertyll.freshly.permission.application.service.query.PermissionQueryService;
import com.vertyll.freshly.permission.domain.repository.RoleAuthorityRepository;

@Configuration
public class ApplicationBeansConfig {
    @Bean
    PermissionQueryUseCase permissionQueryUseCase(RoleAuthorityRepository roles, List<PermissionCatalogue> catalogues) {
        return new PermissionQueryService(roles, catalogues);
    }

    @Bean
    RoleAuthorityCommandUseCase roleAuthorityCommandUseCase(
        TransactionalUseCaseFactory transactions,
        RoleAuthorityRepository roles,
        List<PermissionCatalogue> catalogues
    ) {
        return transactions.readWrite(
            RoleAuthorityCommandUseCase.class,
            new RoleAuthorityCommandService(
                roles,
                catalogues,
                new Slf4jUseCaseLogger(RoleAuthorityCommandService.class)
            )
        );
    }
}
