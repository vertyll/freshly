package com.vertyll.freshly.translation.application.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.i18n.IcuMessages;
import com.vertyll.freshly.lang.logging.RecordingUseCaseLogger;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;
import com.vertyll.freshly.translation.application.command.ImportTranslationsCommand;
import com.vertyll.freshly.translation.application.command.ImportedTranslation;
import com.vertyll.freshly.translation.application.dto.ImportReport;
import com.vertyll.freshly.translation.application.service.command.TranslationImportService;
import com.vertyll.freshly.translation.domain.model.MessageGrammar;
import com.vertyll.freshly.translation.domain.model.TranslationKey;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationImportServiceTest {

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

    private static final String KEY = "error.auth.tokenExpired";
    private static final String SIZE_KEY = "validation.size";
    private static final String EDITOR = "mikolaj";

    private InMemoryTranslations translations;
    private TranslationImportService service;

    @BeforeEach
    void setUp() {
        translations = new InMemoryTranslations();
        translations.save(
            TranslationKey.declare(KEY, "auth", Map.of("en", "This link has expired.", "pl", "Link wygasł."), GRAMMAR)
        );
        translations
            .save(TranslationKey.declare(SIZE_KEY, "web", Map.of("en", "From {min} to {max} characters."), GRAMMAR));
        service = new TranslationImportService(translations, GRAMMAR, new RecordingUseCaseLogger());
    }

    @Test
    @DisplayName("a changed cell becomes an override")
    void appliesAnEdit() {
        ImportReport report = importing(new ImportedTranslation(KEY, "pl", "Odnośnik stracił ważność.", 2));

        assertThat(report.applied()).isEqualTo(1);
        assertThat(translations.require(KEY).resolve("pl")).contains("Odnośnik stracił ważność.");
    }

    @Test
    @DisplayName("a cell left at the default is not stored as an override")
    void roundTripChangesNothing() {
        ImportReport report = importing(
            new ImportedTranslation(KEY, "en", "This link has expired.", 2),
            new ImportedTranslation(KEY, "pl", "Link wygasł.", 2)
        );

        assertThat(report.applied()).isZero();
        assertThat(report.unchanged()).isEqualTo(2);
        assertThat(translations.require(KEY).overrides()).isEmpty();
    }

    @Test
    @DisplayName("restoring a cell to the default clears the override")
    void restoringClearsTheOverride() {
        TranslationKey key = translations.require(KEY);
        key.override("pl", "Coś innego.", EDITOR, GRAMMAR);

        ImportReport report = importing(new ImportedTranslation(KEY, "pl", "Link wygasł.", 2));

        assertThat(report.cleared()).isEqualTo(1);
        assertThat(translations.require(KEY).overrides()).isEmpty();
    }

    @Test
    @DisplayName("a key no module declares is skipped and reported")
    void skipsUnknownKeys() {
        ImportReport report = importing(new ImportedTranslation("error.auth.invented", "pl", "Cokolwiek.", 9));

        assertThat(report.unknownKeys()).contains("error.auth.invented");
        assertThat(report.applied()).isZero();
    }

    @Test
    @DisplayName("an unsupported language column is skipped and reported")
    void skipsUnknownLanguages() {
        ImportReport report = importing(new ImportedTranslation(KEY, "de", "Abgelaufen.", 2));

        assertThat(report.unknownLanguages()).contains("de");
        assertThat(report.applied()).isZero();
    }

    @Test
    @DisplayName("a pattern that will not compile is rejected, naming its row")
    void rejectsMalformedPattern() {
        ImportReport report = importing(new ImportedTranslation(KEY, "pl", "Zostało {count, plural, one{dzień}", 14));

        assertThat(report.rejected()).hasSize(1);
        assertThat(report.rejected().get(0).rowNumber()).isEqualTo(14);
        assertThat(report.rejected().get(0).code()).isEqualTo("error.translation.invalidPattern");
    }

    @Test
    @DisplayName("an edit that drops a placeholder is rejected")
    void rejectsPlaceholderDrift() {
        ImportReport report = importing(new ImportedTranslation(SIZE_KEY, "en", "Too long.", 3));

        assertThat(report.rejected()).hasSize(1);
        assertThat(report.rejected().get(0).code()).isEqualTo("error.translation.placeholderMismatch");
        assertThat(translations.require(SIZE_KEY).overrides()).isEmpty();
    }

    @Test
    @DisplayName("one bad row does not stop the others")
    void oneBadRowDoesNotStopTheRest() {
        ImportReport report = importing(
            new ImportedTranslation(SIZE_KEY, "en", "Too long.", 3),
            new ImportedTranslation(KEY, "pl", "Odnośnik stracił ważność.", 4)
        );

        assertThat(report.rejected()).hasSize(1);
        assertThat(report.applied()).isEqualTo(1);
    }

    @Test
    @DisplayName("what is still untranslated is reported across the whole catalogue")
    void reportsWhatIsStillMissing() {
        ImportReport report = importing(new ImportedTranslation(KEY, "pl", "Odnośnik stracił ważność.", 2));

        assertThat(report.missing()).contains(new ImportReport.MissingTranslation(SIZE_KEY, "pl"));
    }

    private ImportReport importing(ImportedTranslation... rows) {
        return service.importOverrides(new ImportTranslationsCommand(List.of(rows), EDITOR));
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
