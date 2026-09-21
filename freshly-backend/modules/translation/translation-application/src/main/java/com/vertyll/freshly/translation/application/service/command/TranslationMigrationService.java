package com.vertyll.freshly.translation.application.service.command;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import com.vertyll.freshly.lang.i18n.TranslationMigration;
import com.vertyll.freshly.lang.i18n.TranslationMigrationOperations;
import com.vertyll.freshly.lang.logging.UseCaseLogger;
import com.vertyll.freshly.translation.application.port.inbound.command.TranslationMigrationUseCase;
import com.vertyll.freshly.translation.domain.model.TranslationKey;
import com.vertyll.freshly.translation.domain.repository.AppliedMigrationRepository;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;

public class TranslationMigrationService implements TranslationMigrationUseCase, TranslationMigrationOperations {
    private final TranslationRepository translations;
    private final AppliedMigrationRepository applied;
    private final UseCaseLogger logger;

    public TranslationMigrationService(
        TranslationRepository translations,
        AppliedMigrationRepository applied,
        UseCaseLogger logger
    ) {
        this.translations = translations;
        this.applied = applied;
        this.logger = logger;
    }

    @Override
    public boolean runIfPending(TranslationMigration migration) {
        if (applied.isApplied(migration.id())) {
            return false;
        }

        migration.apply(this);
        applied.markApplied(migration.id(), migration.context(), Instant.now());

        logger.info("Applied translation migration {} from {}", migration.id(), migration.context());
        return true;
    }

    @Override
    public void rename(String fromKey, String toKey) {
        if (fromKey.equals(toKey)) {
            return;
        }

        Optional<TranslationKey> found = translations.findByKey(fromKey);
        if (found.isEmpty()) {
            return;
        }
        TranslationKey source = found.get();

        TranslationKey target = translations.findByKey(toKey)
            .orElseGet(
                () -> TranslationKey.reconstitute(toKey, source.context(), false, source.defaults(), Map.of(), null)
            );
        target.adoptOverridesFrom(source);

        translations.save(target);
        translations.deleteByKey(fromKey);
    }

    @Override
    public void retire(String key) {
        translations.deleteByKey(key);
    }

    @Override
    public void reassign(String key, String toContext) {
        translations.findByKey(key).ifPresent(found -> {
            found.reassignTo(toContext);
            translations.save(found);
        });
    }
}
