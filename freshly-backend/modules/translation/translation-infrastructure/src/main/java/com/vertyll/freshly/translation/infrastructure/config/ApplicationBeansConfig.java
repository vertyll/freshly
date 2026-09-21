package com.vertyll.freshly.translation.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.infra.logging.Slf4jUseCaseLogger;
import com.vertyll.freshly.infra.transaction.TransactionalUseCaseFactory;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationAdminUseCase;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationImportUseCase;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationMigrationUseCase;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationRegistrationUseCase;
import com.vertyll.freshly.translation.application.port.inbound.query.TranslationExportUseCase;
import com.vertyll.freshly.translation.application.port.inbound.query.TranslationQueryUseCase;
import com.vertyll.freshly.translation.application.service.command.TranslationAdminService;
import com.vertyll.freshly.translation.application.service.command.TranslationImportService;
import com.vertyll.freshly.translation.application.service.command.TranslationMigrationService;
import com.vertyll.freshly.translation.application.service.command.TranslationRegistrationService;
import com.vertyll.freshly.translation.application.service.query.TranslationExportService;
import com.vertyll.freshly.translation.application.service.query.TranslationQueryService;
import com.vertyll.freshly.translation.domain.model.MessageGrammar;
import com.vertyll.freshly.translation.domain.repository.AppliedMigrationRepository;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;

@Configuration
public class ApplicationBeansConfig {
    @Bean
    TranslationQueryUseCase translationQueryUseCase(
        TransactionalUseCaseFactory transactions,
        TranslationRepository translations
    ) {
        return transactions.readOnly(TranslationQueryUseCase.class, new TranslationQueryService(translations));
    }

    @Bean
    TranslationExportUseCase translationExportUseCase(
        TransactionalUseCaseFactory transactions,
        TranslationRepository translations
    ) {
        return transactions.readOnly(TranslationExportUseCase.class, new TranslationExportService(translations));
    }

    @Bean
    TranslationAdminUseCase translationAdminUseCase(
        TransactionalUseCaseFactory transactions,
        TranslationRepository translations,
        MessageGrammar grammar
    ) {
        return transactions.readWrite(
            TranslationAdminUseCase.class,
            new TranslationAdminService(translations, grammar, new Slf4jUseCaseLogger(TranslationAdminService.class))
        );
    }

    @Bean
    TranslationImportUseCase translationImportUseCase(
        TransactionalUseCaseFactory transactions,
        TranslationRepository translations,
        MessageGrammar grammar
    ) {
        return transactions.readWrite(
            TranslationImportUseCase.class,
            new TranslationImportService(translations, grammar, new Slf4jUseCaseLogger(TranslationImportService.class))
        );
    }

    @Bean
    TranslationMigrationUseCase translationMigrationUseCase(
        TransactionalUseCaseFactory transactions,
        TranslationRepository translations,
        AppliedMigrationRepository applied
    ) {
        return transactions.readWrite(
            TranslationMigrationUseCase.class,
            new TranslationMigrationService(
                translations,
                applied,
                new Slf4jUseCaseLogger(TranslationMigrationService.class)
            )
        );
    }

    @Bean
    TranslationRegistrationUseCase translationRegistrationUseCase(
        TransactionalUseCaseFactory transactions,
        TranslationRepository translations,
        MessageGrammar grammar
    ) {
        return transactions.readWrite(
            TranslationRegistrationUseCase.class,
            new TranslationRegistrationService(
                translations,
                grammar,
                new Slf4jUseCaseLogger(TranslationRegistrationService.class)
            )
        );
    }
}
