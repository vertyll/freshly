package com.vertyll.freshly.translation.application.service.command;

import java.util.Map;

import com.vertyll.freshly.lang.concurrency.VersionGuard;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.UseCaseLogger;
import com.vertyll.freshly.translation.application.command.OverrideTranslationCommand;
import com.vertyll.freshly.translation.application.dto.TranslationEntry;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationAdminUseCase;
import com.vertyll.freshly.translation.domain.error.TranslationError;
import com.vertyll.freshly.translation.domain.model.MessageGrammar;
import com.vertyll.freshly.translation.domain.model.SupportedLanguage;
import com.vertyll.freshly.translation.domain.model.TranslationKey;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;

public class TranslationAdminService implements TranslationAdminUseCase {
    private static final String KEY = "key";

    private final TranslationRepository translations;
    private final MessageGrammar grammar;
    private final UseCaseLogger logger;

    public TranslationAdminService(TranslationRepository translations, MessageGrammar grammar, UseCaseLogger logger) {
        this.translations = translations;
        this.grammar = grammar;
        this.logger = logger;
    }

    @Override
    public TranslationEntry override(OverrideTranslationCommand command) {
        TranslationKey key = require(command.key());
        SupportedLanguage language = SupportedLanguage.require(command.language());

        VersionGuard.requireMatch(
            key.version(),
            command.expectedVersion(),
            () -> new DomainException(TranslationError.VERSION_MISMATCH, Map.of(KEY, command.key()))
        );

        key.override(language.tag(), command.text(), command.author(), grammar);
        TranslationKey saved = translations.save(key);

        logger.info("Translation {} [{}] overridden by {}", command.key(), language.tag(), command.author());
        return TranslationEntry.from(saved, SupportedLanguage.tags());
    }

    @Override
    public TranslationEntry clearOverride(String key, String language) {
        TranslationKey found = require(key);
        SupportedLanguage supported = SupportedLanguage.require(language);

        found.clearOverride(supported.tag());
        TranslationKey saved = translations.save(found);

        logger.info("Translation override {} [{}] cleared", key, supported.tag());
        return TranslationEntry.from(saved, SupportedLanguage.tags());
    }

    private TranslationKey require(String key) {
        return translations.findByKey(key)
            .orElseThrow(() -> new DomainException(TranslationError.KEY_NOT_FOUND, Map.of(KEY, key)));
    }
}
