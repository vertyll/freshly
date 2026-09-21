package com.vertyll.freshly.translation.infrastructure.config;

import java.util.Comparator;
import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.vertyll.freshly.lang.i18n.TranslationMigration;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationMigrationUseCase;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class TranslationMigrationRunner {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    ApplicationRunner runTranslationMigrations(
        TranslationMigrationUseCase migrations,
        List<TranslationMigration> declared
    ) {
        return args -> {
            int ran = 0;

            for (TranslationMigration migration : declared.stream()
                .sorted(Comparator.comparing(TranslationMigration::id))
                .toList()) {
                try {
                    if (migrations.runIfPending(migration)) {
                        ran++;
                    }
                } catch (RuntimeException e) {
                    log.error("Translation migration {} failed", migration.id(), e);
                }
            }

            log.info("Translation migrations: {} applied, {} declared", ran, declared.size());
        };
    }
}
