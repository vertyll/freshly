package com.vertyll.freshly.translation.application.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.UseCaseLogger;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationRegistrationUseCase;
import com.vertyll.freshly.translation.domain.model.MessageGrammar;
import com.vertyll.freshly.translation.domain.model.TranslationKey;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;

public class TranslationRegistrationService implements TranslationRegistrationUseCase {
    private final TranslationRepository translations;
    private final MessageGrammar grammar;
    private final UseCaseLogger logger;

    public TranslationRegistrationService(
        TranslationRepository translations,
        MessageGrammar grammar,
        UseCaseLogger logger
    ) {
        this.translations = translations;
        this.grammar = grammar;
        this.logger = logger;
    }

    @Override
    public int registerDefaults(String context, Map<String, Map<String, String>> defaults) {
        Map<String, TranslationKey> existingByKey = translations.findAllByKeys(defaults.keySet());
        List<TranslationKey> toSave = new ArrayList<>(defaults.size());
        List<String> refused = new ArrayList<>();

        defaults.forEach((key, texts) -> {
            try {
                toSave.add(register(context, key, texts, existingByKey.get(key)));
            } catch (DomainException e) {
                refused.add(key);
                logger.error("Refused translation key {} for {}: {}", key, context, e.error().key());
            }
        });

        translations.saveAll(toSave);
        if (refused.isEmpty()) {
            logger.info("Registered {} translation keys for {}", toSave.size(), context);
        } else {
            logger.error("Registered {} translation keys for {}; refused {}", toSave.size(), context, refused);
        }

        return toSave.size();
    }

    @Override
    public int markOrphans(Set<String> declaredKeys) {
        List<TranslationKey> orphaned = new ArrayList<>();

        for (TranslationKey stored : translations.findAll()) {
            if (stored.declared() && !declaredKeys.contains(stored.key())) {
                stored.markOrphaned();
                orphaned.add(stored);
            }
        }

        translations.saveAll(orphaned);
        if (!orphaned.isEmpty()) {
            logger.warn("{} translation keys are no longer declared by any module", orphaned.size());
        }
        return orphaned.size();
    }

    private TranslationKey register(
        String context,
        String key,
        Map<String, String> texts,
        @Nullable TranslationKey existing
    ) {
        if (existing == null) {
            return TranslationKey.declare(key, context, texts, grammar);
        }
        existing.refreshDefaults(context, texts, grammar);
        return existing;
    }
}
