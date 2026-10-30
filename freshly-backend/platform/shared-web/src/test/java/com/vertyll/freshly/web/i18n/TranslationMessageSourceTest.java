package com.vertyll.freshly.web.i18n;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranslationMessageSourceTest {
    private static final Object[] NO_ARGUMENTS = {};

    private final TranslationMessageSource source = new TranslationMessageSource(
        (key, language) -> Optional.ofNullable(
            Map.of("en:greeting", "Hello {0}", "pl:greeting", "Witaj {0}", "en:plain", "Plain")
                .get(language + ":" + key)
        )
    );

    @AfterEach
    void noReader() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void aMessageIsFormattedWithItsArguments() {
        assertThat(
            source.getMessage(
                "greeting",
                new Object[] {
                    "Ada"
                },
                Locale.ENGLISH
            )
        ).isEqualTo("Hello Ada");
    }

    @Test
    void aMessageWithoutArgumentsIsTheTextItself() {
        assertThat(source.getMessage("plain", NO_ARGUMENTS, Locale.ENGLISH)).isEqualTo("Plain");
    }

    @Test
    void withoutALocaleTheReadersLanguageIsUsed() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));

        assertThat(
            source.getMessage(
                "greeting",
                new Object[] {
                    "Ada"
                },
                (Locale) null
            )
        ).isEqualTo("Witaj Ada");
    }

    @Test
    void anUnknownCodeGivesTheDefaultMessageWhenThereIsOne() {
        assertThat(source.getMessage("missing", NO_ARGUMENTS, "fallback", Locale.ENGLISH)).isEqualTo("fallback");
    }

    @Test
    void aKnownCodeIsPreferredToTheDefaultMessage() {
        assertThat(source.getMessage("plain", NO_ARGUMENTS, "fallback", Locale.ENGLISH)).isEqualTo("Plain");
    }

    @Test
    void anUnknownCodeWithoutADefaultIsAnError() {
        assertThatThrownBy(() -> source.getMessage("missing", NO_ARGUMENTS, Locale.ENGLISH))
            .isInstanceOf(NoSuchMessageException.class);
    }

    @Test
    void theFirstCodeWithTextWins() {
        DefaultMessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
            new String[] {
                "missing",
                "plain"
            }
        );

        assertThat(source.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("Plain");
    }

    @Test
    void whenNoCodeHasTextTheDefaultMessageIsUsed() {
        DefaultMessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
            new String[] {
                "missing"
            },
            "fallback"
        );

        assertThat(source.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("fallback");
    }

    @Test
    void whenNothingIsKnownTheLastCodeIsNamed() {
        DefaultMessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
            new String[] {
                "first",
                "last"
            }
        );

        assertThatThrownBy(() -> source.getMessage(resolvable, Locale.ENGLISH))
            .isInstanceOf(NoSuchMessageException.class)
            .hasMessageContaining("last");
    }

    @Test
    void aResolvableWithNoCodesAndNoDefaultIsAnError() {
        DefaultMessageSourceResolvable resolvable = new DefaultMessageSourceResolvable((String[]) null);

        assertThatThrownBy(() -> source.getMessage(resolvable, Locale.ENGLISH))
            .isInstanceOf(NoSuchMessageException.class);
    }
}
