package com.vertyll.freshly.translation.application.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.i18n.IcuMessages;
import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.RecordingUseCaseLogger;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;
import com.vertyll.freshly.lang.page.PagedResponse;
import com.vertyll.freshly.translation.application.command.OverrideTranslationCommand;
import com.vertyll.freshly.translation.application.dto.TranslationBundle;
import com.vertyll.freshly.translation.application.dto.TranslationEntry;
import com.vertyll.freshly.translation.application.service.command.TranslationAdminService;
import com.vertyll.freshly.translation.application.service.query.TranslationQueryService;
import com.vertyll.freshly.translation.domain.error.TranslationError;
import com.vertyll.freshly.translation.domain.model.MessageGrammar;
import com.vertyll.freshly.translation.domain.model.TranslationKey;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranslationQueriesAndAdminTest {
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
    private static final String EDITOR = "editor";
    private static final Long STORED_VERSION = 3L;

    private Translations translations;
    private TranslationQueryService queries;
    private TranslationAdminService admin;

    @BeforeEach
    void aCatalogueWithOneKey() {
        translations = new Translations();
        translations.save(
            TranslationKey.reconstitute(
                KEY,
                "auth",
                true,
                Map.of("en", "Link expired.", "pl", "Link wygasł."),
                Map.of(),
                STORED_VERSION
            )
        );
        queries = new TranslationQueryService(translations);
        admin = new TranslationAdminService(translations, GRAMMAR, new RecordingUseCaseLogger());
    }

    private static void assertRefused(Runnable action, DomainError error) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.error()).isEqualTo(error));
    }

    private TranslationEntry overrideInPolish(String text, Long expectedVersion) {
        return admin.override(new OverrideTranslationCommand(KEY, "pl", text, EDITOR, expectedVersion));
    }

    @Test
    void aBundleHoldsTheTextsOfItsLanguage() {
        TranslationBundle bundle = queries.bundleFor("pl");

        assertThat(bundle.language()).isEqualTo("pl");
        assertThat(bundle.messages()).containsEntry(KEY, "Link wygasł.");
    }

    @Test
    void aBundleInAnUnsupportedLanguageIsRefused() {
        assertRefused(() -> queries.bundleFor("xx"), TranslationError.UNSUPPORTED_LANGUAGE);
    }

    @Test
    void theEtagOfABundleChangesOnlyWithItsTexts() {
        String before = queries.bundleFor("pl").etag();

        assertThat(queries.bundleFor("pl").etag()).isEqualTo(before);

        overrideInPolish("Odnośnik wygasł.", STORED_VERSION);

        assertThat(queries.bundleFor("pl").etag()).isNotEqualTo(before);
    }

    @Test
    void anOverrideWinsOverTheDefault() {
        overrideInPolish("Odnośnik wygasł.", STORED_VERSION);

        assertThat(queries.resolve(KEY, "pl")).contains("Odnośnik wygasł.");
        assertThat(queries.resolve(KEY, "en")).contains("Link expired.");
    }

    @Test
    void anUnknownKeyResolvesToNothing() {
        assertThat(queries.resolve("error.unknown", "pl")).isEmpty();
    }

    @Test
    void anEntryShowsTheOverrideBesideTheDefaultAndWhoMadeIt() {
        TranslationEntry entry = overrideInPolish("Odnośnik wygasł.", null);

        TranslationEntry.LanguageValue polish = entry.values().get("pl");
        assertThat(polish.defaultText()).isEqualTo("Link wygasł.");
        assertThat(polish.overrideText()).isEqualTo("Odnośnik wygasł.");
        assertThat(polish.author()).isEqualTo(EDITOR);
        assertThat(entry.values().get("en").overrideText()).isNull();
    }

    @Test
    void anOverrideAgainstAnOlderVersionIsRefused() {
        assertRefused(() -> overrideInPolish("Inny tekst.", 1L), TranslationError.VERSION_MISMATCH);
    }

    @Test
    void anOverrideOfAnUnknownKeyIsRefused() {
        OverrideTranslationCommand command = new OverrideTranslationCommand("error.unknown", "pl", "x", EDITOR, null);

        assertRefused(() -> admin.override(command), TranslationError.KEY_NOT_FOUND);
    }

    @Test
    void anOverrideInAnUnsupportedLanguageIsRefused() {
        OverrideTranslationCommand command = new OverrideTranslationCommand(KEY, "xx", "x", EDITOR, null);

        assertRefused(() -> admin.override(command), TranslationError.UNSUPPORTED_LANGUAGE);
    }

    @Test
    void clearingAnOverrideBringsTheDefaultBack() {
        overrideInPolish("Odnośnik wygasł.", STORED_VERSION);

        TranslationEntry entry = admin.clearOverride(KEY, "pl");

        assertThat(entry.values().get("pl").overrideText()).isNull();
        assertThat(queries.resolve(KEY, "pl")).contains("Link wygasł.");
    }

    @Test
    void listingAndSearchingGiveEntriesWithTheirCounts() {
        PagedResponse<TranslationEntry> listed = queries.list(new PageRequest(0, 10));
        PagedResponse<TranslationEntry> found = queries.search("tokenExpired", new PageRequest(0, 10));

        assertThat(listed.items()).extracting(TranslationEntry::key).containsExactly(KEY);
        assertThat(found.pagination().total()).isEqualTo(1);
    }

    @Test
    void anOverrideOfADefaultThatChangedIsStale() {
        overrideInPolish("Odnośnik wygasł.", STORED_VERSION);
        translations.findByKey(KEY)
            .orElseThrow()
            .refreshDefaults("auth", Map.of("en", "Link expired.", "pl", "Link stracił ważność."), GRAMMAR);

        assertThat(queries.staleOverrides()).singleElement().satisfies(entry -> {
            assertThat(entry.hasStaleOverride()).isTrue();
            assertThat(entry.values().get("pl").stale()).isTrue();
        });
    }

    @Test
    void aKeyNoContextDeclaresIsAnOrphan() {
        assertThat(queries.orphans()).isEmpty();

        translations.findByKey(KEY).orElseThrow().markOrphaned();

        assertThat(queries.orphans()).extracting(TranslationEntry::key).containsExactly(KEY);
    }

    @Test
    void theSupportedLanguagesAreKnown() {
        assertThat(queries.supportedLanguages()).containsExactlyInAnyOrder("en", "pl");
    }

    private static final class Translations implements TranslationRepository {
        private final Map<String, TranslationKey> keys = new LinkedHashMap<>();

        @Override
        public TranslationKey save(TranslationKey translationKey) {
            keys.put(translationKey.key(), translationKey);
            return translationKey;
        }

        @Override
        public List<TranslationKey> saveAll(Collection<TranslationKey> translationKeys) {
            translationKeys.forEach(this::save);
            return List.copyOf(translationKeys);
        }

        @Override
        public Optional<TranslationKey> findByKey(String key) {
            return Optional.ofNullable(keys.get(key));
        }

        @Override
        public Map<String, TranslationKey> findAllByKeys(Collection<String> wanted) {
            Map<String, TranslationKey> found = new LinkedHashMap<>(keys);
            found.keySet().retainAll(wanted);
            return found;
        }

        @Override
        public List<TranslationKey> findByContext(String context) {
            return keys.values().stream().filter(key -> key.context().equals(context)).toList();
        }

        @Override
        public List<TranslationKey> findAll() {
            return List.copyOf(keys.values());
        }

        @Override
        public List<TranslationKey> findOrphaned() {
            return keys.values().stream().filter(key -> !key.declared()).toList();
        }

        @Override
        public void deleteByKey(String key) {
            keys.remove(key);
        }

        @Override
        public PageResult<TranslationKey> findAll(PageRequest pageRequest) {
            return new PageResult<>(findAll(), pageRequest.page(), pageRequest.size(), keys.size());
        }

        @Override
        public PageResult<TranslationKey> search(String fragment, PageRequest pageRequest) {
            List<TranslationKey> found = keys.values().stream().filter(key -> key.key().contains(fragment)).toList();
            return new PageResult<>(found, pageRequest.page(), pageRequest.size(), found.size());
        }
    }
}
