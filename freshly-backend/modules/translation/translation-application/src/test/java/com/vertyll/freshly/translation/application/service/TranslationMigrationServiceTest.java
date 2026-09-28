package com.vertyll.freshly.translation.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.i18n.IcuMessages;
import com.vertyll.freshly.lang.i18n.TranslationMigration;
import com.vertyll.freshly.lang.i18n.TranslationMigrationOperations;
import com.vertyll.freshly.lang.logging.RecordingUseCaseLogger;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;
import com.vertyll.freshly.translation.application.service.command.TranslationMigrationService;
import com.vertyll.freshly.translation.domain.model.MessageGrammar;
import com.vertyll.freshly.translation.domain.model.TranslationKey;
import com.vertyll.freshly.translation.domain.repository.AppliedMigrationRepository;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationMigrationServiceTest {

    private static final MessageGrammar GRAMMAR = new MessageGrammar() {
        @Override
        public Optional<String> rejectionReason(String languageTag, String text) {
            return IcuMessages.validate(languageTag, text);
        }

        @Override
        public Set<String> placeholdersOf(String languageTag, String text) {
            return IcuMessages.argumentsOf(languageTag, text);
        }
    };

    private static final String OLD_KEY = "error.auth.tokenExpired";
    private static final String NEW_KEY = "error.auth.linkExpired";

    private InMemoryTranslations translations;
    private InMemoryApplied applied;
    private TranslationMigrationService service;

    @BeforeEach
    void setUp() {
        translations = new InMemoryTranslations();
        applied = new InMemoryApplied();
        translations.save(
            TranslationKey
                .declare(OLD_KEY, "auth", Map.of("en", "This link has expired.", "pl", "Link wygasł."), GRAMMAR)
        );
        service = new TranslationMigrationService(translations, applied, new RecordingUseCaseLogger());
    }

    @Test
    @DisplayName("renaming carries the override to the new key")
    void renameCarriesOverrides() {
        translations.require(OLD_KEY).override("pl", "Odnośnik stracił ważność.", "mikolaj", GRAMMAR);

        run(operations -> operations.rename(OLD_KEY, NEW_KEY));

        assertThat(translations.require(OLD_KEY)).isNull();
        assertThat(translations.require(NEW_KEY).resolve("pl")).contains("Odnośnik stracił ważność.");
    }

    @Test
    @DisplayName("the renamed key is an orphan until its module declares it")
    void renamedKeyIsNotDeclared() {
        run(operations -> operations.rename(OLD_KEY, NEW_KEY));

        assertThat(translations.require(NEW_KEY).declared()).isFalse();
    }

    @Test
    @DisplayName("an override already on the target wins over the one being carried")
    void targetOverrideWins() {
        translations.save(TranslationKey.declare(NEW_KEY, "auth", Map.of("pl", "Link wygasł."), GRAMMAR));
        translations.require(NEW_KEY).override("pl", "Nowszy tekst.", "mikolaj", GRAMMAR);
        translations.require(OLD_KEY).override("pl", "Starszy tekst.", "mikolaj", GRAMMAR);

        run(operations -> operations.rename(OLD_KEY, NEW_KEY));

        assertThat(translations.require(NEW_KEY).resolve("pl")).contains("Nowszy tekst.");
    }

    @Test
    @DisplayName("renaming a key that is already gone does nothing")
    void renameIsANoOpWhenAlreadyDone() {
        run(operations -> operations.rename("error.auth.neverExisted", NEW_KEY));

        assertThat(translations.require(NEW_KEY)).isNull();
    }

    @Test
    @DisplayName("renaming a key to itself keeps it, overrides and all")
    void renameToItselfKeepsTheKey() {
        translations.require(OLD_KEY).override("pl", "Odnośnik stracił ważność.", "mikolaj", GRAMMAR);

        run(operations -> operations.rename(OLD_KEY, OLD_KEY));

        assertThat(translations.require(OLD_KEY)).isNotNull();
        assertThat(translations.require(OLD_KEY).resolve("pl")).contains("Odnośnik stracił ważność.");
    }

    @Test
    @DisplayName("retiring removes the key")
    void retireRemoves() {
        run(operations -> operations.retire(OLD_KEY));

        assertThat(translations.require(OLD_KEY)).isNull();
    }

    @Test
    @DisplayName("reassigning hands the key to another context")
    void reassignChangesOwner() {
        run(operations -> operations.reassign(OLD_KEY, "useraccess"));

        assertThat(translations.require(OLD_KEY).context()).isEqualTo("useraccess");
    }

    @Test
    @DisplayName("a migration that has run is not run again")
    void runsOnce() {
        Migration migration = new Migration(operations -> operations.retire(OLD_KEY));

        assertThat(service.runIfPending(migration)).isTrue();
        assertThat(service.runIfPending(migration)).isFalse();
        assertThat(migration.applications).isEqualTo(1);
    }

    @Test
    @DisplayName("the id is recorded so the next start-up skips it")
    void recordsTheId() {
        run(operations -> operations.retire(OLD_KEY));

        assertThat(applied.isApplied("test-migration")).isTrue();
    }

    private void run(Consumer<TranslationMigrationOperations> body) {
        service.runIfPending(new Migration(body));
    }

    private static final class Migration implements TranslationMigration {
        private final Consumer<TranslationMigrationOperations> body;
        private int applications;

        private Migration(Consumer<TranslationMigrationOperations> body) {
            this.body = body;
        }

        @Override
        public String id() {
            return "test-migration";
        }

        @Override
        public String context() {
            return "auth";
        }

        @Override
        public void apply(TranslationMigrationOperations operations) {
            applications++;
            body.accept(operations);
        }
    }

    private static final class InMemoryApplied implements AppliedMigrationRepository {
        private final Set<String> ids = new HashSet<>();

        @Override
        public boolean isApplied(String migrationId) {
            return ids.contains(migrationId);
        }

        @Override
        public void markApplied(String migrationId, String context, Instant at) {
            ids.add(migrationId);
        }
    }

    private static final class InMemoryTranslations implements TranslationRepository {
        private final Map<String, TranslationKey> stored = new LinkedHashMap<>();

        TranslationKey require(String key) {
            return stored.get(key);
        }

        @Override
        public TranslationKey save(TranslationKey translationKey) {
            stored.put(translationKey.key(), translationKey);
            return translationKey;
        }

        @Override
        public List<TranslationKey> saveAll(Collection<TranslationKey> translationKeys) {
            translationKeys.forEach(this::save);
            return new ArrayList<>(translationKeys);
        }

        @Override
        public Optional<TranslationKey> findByKey(String key) {
            return Optional.ofNullable(stored.get(key));
        }

        @Override
        public Map<String, TranslationKey> findAllByKeys(Collection<String> keys) {
            Map<String, TranslationKey> found = new LinkedHashMap<>();
            keys.forEach(key -> {
                TranslationKey value = stored.get(key);
                if (value != null) {
                    found.put(key, value);
                }
            });
            return found;
        }

        @Override
        public List<TranslationKey> findByContext(String context) {
            return stored.values().stream().filter(key -> key.context().equals(context)).toList();
        }

        @Override
        public List<TranslationKey> findOrphaned() {
            return stored.values().stream().filter(key -> !key.declared()).toList();
        }

        @Override
        public void deleteByKey(String key) {
            stored.remove(key);
        }

        @Override
        public List<TranslationKey> findAll() {
            return new ArrayList<>(stored.values());
        }

        @Override
        public PageResult<TranslationKey> findAll(PageRequest pageRequest) {
            return PageResult.empty(pageRequest);
        }

        @Override
        public PageResult<TranslationKey> search(String fragment, PageRequest pageRequest) {
            return PageResult.empty(pageRequest);
        }
    }
}
