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
import com.vertyll.freshly.lang.logging.UseCaseLogger;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;
import com.vertyll.freshly.translation.application.service.command.TranslationRegistrationService;
import com.vertyll.freshly.translation.domain.model.MessageGrammar;
import com.vertyll.freshly.translation.domain.model.TranslationKey;
import com.vertyll.freshly.translation.domain.repository.TranslationRepository;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationRegistrationServiceTest {

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
    private static final String CONTEXT = "auth";

    private InMemoryTranslations translations;
    private TranslationRegistrationService service;

    @BeforeEach
    void setUp() {
        translations = new InMemoryTranslations();
        service = new TranslationRegistrationService(translations, GRAMMAR, new NoOpLogger());
    }

    @Test
    @DisplayName("re-registering on start-up does not overwrite an administrator's override")
    void redeployKeepsOverrides() {
        service.registerDefaults(CONTEXT, Map.of(KEY, Map.of("pl", "Link wygasł.")));

        TranslationKey stored = translations.require(KEY);
        stored.override("pl", "Ten odnośnik stracił ważność.", "mikolaj", GRAMMAR);
        translations.save(stored);

        service.registerDefaults(CONTEXT, Map.of(KEY, Map.of("pl", "Link nie jest już ważny.")));

        assertThat(translations.require(KEY).resolve("pl")).contains("Ten odnośnik stracił ważność.");
    }

    @Test
    @DisplayName("an improved default reaches everyone who has not overridden it")
    void improvedDefaultPropagates() {
        service.registerDefaults(CONTEXT, Map.of(KEY, Map.of("en", "Expired.")));

        service.registerDefaults(CONTEXT, Map.of(KEY, Map.of("en", "This link has expired.")));

        assertThat(translations.require(KEY).resolve("en")).contains("This link has expired.");
    }

    @Test
    @DisplayName("a key added by a later release is registered on the next start-up")
    void registersLateArrivals() {
        service.registerDefaults(CONTEXT, Map.of(KEY, Map.of("en", "Expired.")));

        service.registerDefaults(
            CONTEXT,
            Map.of(KEY, Map.of("en", "Expired."), "error.auth.tokenInvalid", Map.of("en", "Not valid."))
        );

        assertThat(translations.stored).hasSize(2);
    }

    @Test
    @DisplayName("another context may not claim a key that is already owned")
    void refusesForeignClaim() {
        service.registerDefaults(CONTEXT, Map.of(KEY, Map.of("en", "Expired.")));

        int registered = service.registerDefaults("useraccess", Map.of(KEY, Map.of("en", "Other")));

        assertThat(registered).isZero();
        assertThat(translations.require(KEY).context()).isEqualTo(CONTEXT);
        assertThat(translations.require(KEY).resolve("en")).contains("Expired.");
    }

    @Test
    @DisplayName("loads the context in one query rather than one per key")
    void loadsInOneQuery() {
        Map<String, Map<String, String>> many = new LinkedHashMap<>();
        for (int i = 0; i < 50; i++) {
            many.put("error.auth.key" + i, Map.of("en", "Text " + i));
        }

        service.registerDefaults(CONTEXT, many);

        assertThat(translations.bulkLoads).isEqualTo(1);
        assertThat(translations.singleLoads).isZero();
    }

    @Test
    @DisplayName("one refused key does not cost the module its others")
    void oneRefusedKeyDoesNotStopTheRest() {
        TranslationKey owned =
                TranslationKey.declare("error.auth.ownedElsewhere", "useraccess", Map.of("en", "Theirs"), GRAMMAR);
        translations.save(owned);

        Map<String, Map<String, String>> defaults = new LinkedHashMap<>();
        defaults.put("error.auth.ownedElsewhere", Map.of("en", "Ours"));
        defaults.put("error.auth.fine", Map.of("en", "Fine"));

        int registered = service.registerDefaults(CONTEXT, defaults);

        assertThat(registered).isEqualTo(1);
        assertThat(translations.require("error.auth.fine")).isNotNull();
        assertThat(translations.require("error.auth.ownedElsewhere").context()).isEqualTo("useraccess");
    }

    @Test
    @DisplayName("a key no module declares any more is marked an orphan")
    void marksOrphans() {
        service.registerDefaults(CONTEXT, Map.of(KEY, Map.of("en", "Text")));
        translations.save(TranslationKey.declare("error.auth.removed", CONTEXT, Map.of("en", "Gone"), GRAMMAR));

        int orphaned = service.markOrphans(Set.of(KEY));

        assertThat(orphaned).isEqualTo(1);
        assertThat(translations.require("error.auth.removed").declared()).isFalse();
        assertThat(translations.require(KEY).declared()).isTrue();
    }

    @Test
    @DisplayName("re-declaring an orphaned key brings it back")
    void redeclaringClearsTheOrphanMark() {
        service.registerDefaults(CONTEXT, Map.of(KEY, Map.of("en", "Text")));
        service.markOrphans(Set.of());

        service.registerDefaults(CONTEXT, Map.of(KEY, Map.of("en", "Text")));

        assertThat(translations.require(KEY).declared()).isTrue();
    }

    private static final class InMemoryTranslations implements TranslationRepository {
        private final Map<String, TranslationKey> stored = new LinkedHashMap<>();
        private int bulkLoads;
        private int singleLoads;

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
            singleLoads++;
            return Optional.ofNullable(stored.get(key));
        }

        @Override
        public Map<String, TranslationKey> findAllByKeys(Collection<String> keys) {
            bulkLoads++;
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
            return stored.values().stream().filter(k -> k.context().equals(context)).toList();
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

    private static final class NoOpLogger implements UseCaseLogger {
        @Override
        public void debug(String message, Object... args) {
        }

        @Override
        public void info(String message, Object... args) {
        }

        @Override
        public void warn(String message, Object... args) {
        }

        @Override
        public void error(String message, Object... args) {
        }
    }
}
