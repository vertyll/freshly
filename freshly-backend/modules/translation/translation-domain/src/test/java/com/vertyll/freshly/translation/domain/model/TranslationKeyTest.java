package com.vertyll.freshly.translation.domain.model;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.i18n.IcuMessages;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.translation.domain.error.TranslationError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranslationKeyTest {
    private static final String KEY = "error.auth.tokenExpired";
    private static final String CONTEXT = "auth";

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

    private static TranslationKey key() {
        return TranslationKey
            .declare(KEY, CONTEXT, Map.of("en", "This link has expired.", "pl", "Link wygasł."), GRAMMAR);
    }

    @Nested
    @DisplayName("declaring")
    class Declaring {
        @Test
        @DisplayName("rejects a key that is not dotted segments")
        void rejectsMalformedKey() {
            assertThatThrownBy(() -> TranslationKey.declare("TokenExpired", CONTEXT, Map.of(), GRAMMAR))
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(TranslationError.INVALID_KEY_FORMAT);
        }

        @Test
        @DisplayName("another context may not claim a declared key")
        void refusesForeignClaim() {
            assertThatThrownBy(() -> key().refreshDefaults("useraccess", Map.of("en", "Something else"), GRAMMAR))
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(TranslationError.KEY_OWNED_BY_ANOTHER_CONTEXT);
        }
    }

    @Nested
    @DisplayName("defaults and overrides")
    class DefaultsAndOverrides {
        @Test
        @DisplayName("refreshing defaults leaves an override untouched")
        void refreshKeepsOverride() {
            TranslationKey key = key();
            key.override("pl", "Ten odnośnik wygasł.", "mikolaj", GRAMMAR);

            key.refreshDefaults(
                CONTEXT,
                Map.of("en", "This link has expired.", "pl", "Link stracił ważność."),
                GRAMMAR
            );

            assertThat(key.resolve("pl")).contains("Ten odnośnik wygasł.");
        }

        @Test
        @DisplayName("resolves the default when no override exists")
        void fallsBackToDefault() {
            assertThat(key().resolve("en")).contains("This link has expired.");
        }

        @Test
        @DisplayName("a language with neither default nor override resolves to nothing")
        void noCrossLanguageFallback() {
            TranslationKey key = TranslationKey.declare(KEY, CONTEXT, Map.of("en", "Expired."), GRAMMAR);

            assertThat(key.resolve("pl")).isEmpty();
        }

        @Test
        @DisplayName("refreshing drops a default the module no longer ships")
        void refreshReplacesRatherThanMerges() {
            TranslationKey key = key();

            key.refreshDefaults(CONTEXT, Map.of("en", "This link has expired."), GRAMMAR);

            assertThat(key.resolve("pl")).isEmpty();
        }

        @Test
        @DisplayName("refuses a blank override, which would render as an empty label")
        void refusesBlankOverride() {
            assertThatThrownBy(() -> key().override("pl", "  ", "mikolaj", GRAMMAR))
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(TranslationError.BLANK_OVERRIDE);
        }

        @Test
        @DisplayName("clearing an override falls back to the current default")
        void clearingFallsBack() {
            TranslationKey key = key();
            key.override("pl", "Ten odnośnik wygasł.", "mikolaj", GRAMMAR);

            key.clearOverride("pl");

            assertThat(key.resolve("pl")).contains("Link wygasł.");
        }
    }

    @Nested
    @DisplayName("message patterns")
    class Patterns {
        @Test
        @DisplayName("refuses an override that will not compile")
        void refusesMalformedPattern() {
            assertThatThrownBy(() -> key().override("pl", "Zostało {count, plural, one{dzień}", "mikolaj", GRAMMAR))
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(TranslationError.INVALID_PATTERN);
        }

        @Test
        @DisplayName("refuses an override that drops a placeholder the default uses")
        void refusesPlaceholderDrift() {
            TranslationKey key = TranslationKey
                .declare("validation.size", CONTEXT, Map.of("pl", "Od {min} do {max} znaków."), GRAMMAR);

            assertThatThrownBy(() -> key.override("pl", "Za długie.", "mikolaj", GRAMMAR))
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(TranslationError.PLACEHOLDER_MISMATCH);
        }

        @Test
        @DisplayName("accepts an override that reorders the same placeholders")
        void acceptsReordering() {
            TranslationKey key = TranslationKey
                .declare("validation.size", CONTEXT, Map.of("pl", "Od {min} do {max} znaków."), GRAMMAR);

            key.override("pl", "Najwyżej {max} znaków, najmniej {min}.", "mikolaj", GRAMMAR);

            assertThat(key.resolve("pl")).contains("Najwyżej {max} znaków, najmniej {min}.");
        }

        @Test
        @DisplayName("accepts a plural form the JDK formatter could not express")
        void acceptsPluralRules() {
            TranslationKey key = TranslationKey.declare(
                "airquality.stations.found",
                CONTEXT,
                Map.of("pl", "Znaleziono {count, plural, one{# stację} few{# stacje} many{# stacji} other{# stacji}}."),
                GRAMMAR
            );

            assertThat(key.defaults()).containsKey("pl");
        }
    }

    @Nested
    @DisplayName("staleness")
    class Staleness {
        @Test
        @DisplayName("an override written against a changed default is reported stale")
        void detectsStaleOverride() {
            TranslationKey key = key();
            key.override("en", "Expired — request a new link.", "mikolaj", GRAMMAR);

            key.refreshDefaults(CONTEXT, Map.of("en", "This link is no longer valid.", "pl", "Link wygasł."), GRAMMAR);

            assertThat(key.staleOverrides()).containsOnlyKeys("en");
        }

        @Test
        @DisplayName("a stale override still resolves")
        void staleOverrideStillWins() {
            TranslationKey key = key();
            key.override("en", "Expired — request a new link.", "mikolaj", GRAMMAR);
            key.refreshDefaults(CONTEXT, Map.of("en", "This link is no longer valid."), GRAMMAR);

            assertThat(key.resolve("en")).contains("Expired — request a new link.");
        }

        @Test
        @DisplayName("an override matching the current default is not stale")
        void unchangedIsNotStale() {
            TranslationKey key = key();
            key.override("en", "Anything.", "mikolaj", GRAMMAR);

            assertThat(key.staleOverrides()).isEmpty();
        }
    }
}
