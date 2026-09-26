package com.vertyll.freshly.translation.infrastructure.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.vertyll.freshly.lang.i18n.TranslationCatalogue;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationRegistrationUseCase;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class TranslationCatalogueRegistrar {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    ApplicationRunner registerTranslationCatalogues(
        TranslationRegistrationUseCase registration,
        List<TranslationCatalogue> catalogues
    ) {
        return _ -> {
            Set<String> declaredKeys = new HashSet<>();
            boolean complete = true;
            int total = 0;

            for (TranslationCatalogue catalogue : catalogues) {
                try {
                    total += registration.registerDefaults(catalogue.context(), catalogue.defaults());
                    declaredKeys.addAll(catalogue.defaults().keySet());
                } catch (RuntimeException e) {
                    complete = false;
                    log.error("Could not register translations for {}", catalogue.context(), e);
                }
            }

            log.info("Registered {} translation keys from {} catalogues", total, catalogues.size());

            if (complete) {
                registration.markOrphans(declaredKeys);
            } else {
                log.warn("Skipping the orphan sweep: at least one catalogue did not register");
            }
        };
    }
}
