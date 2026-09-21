package com.vertyll.freshly.translation.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.i18n.TranslationCatalogue;

@Component
public class TranslationTranslationCatalogue implements TranslationCatalogue {
    @Override
    public String context() {
        return "translation";
    }

    @Override
    public Map<String, Map<String, String>> defaults() {
        Map<String, Map<String, String>> defaults = new LinkedHashMap<>();

        defaults.put(
            "error.translation.keyNotFound",
            Map.of("en", "No such translation key.", "pl", "Nie ma takiego klucza tłumaczenia.")
        );
        defaults.put(
            "error.translation.overrideNotFound",
            Map.of(
                "en",
                "This key has no override in that language.",
                "pl",
                "Ten klucz nie ma nadpisania w tym języku."
            )
        );
        defaults.put(
            "error.translation.invalidKeyFormat",
            Map.of(
                "en",
                "A translation key must be dotted segments, for example error.auth.tokenExpired.",
                "pl",
                "Klucz tłumaczenia musi składać się z segmentów oddzielonych kropkami, np. error.auth.tokenExpired."
            )
        );
        defaults.put(
            "error.translation.blankOverride",
            Map.of(
                "en",
                "An override cannot be blank. Remove it instead to fall back to the default.",
                "pl",
                "Nadpisanie nie może być puste. Usuń je, aby wrócić do wartości domyślnej."
            )
        );
        defaults.put(
            "error.translation.unsupportedLanguage",
            Map.of("en", "That language is not supported.", "pl", "Ten język nie jest obsługiwany.")
        );
        defaults.put(
            "error.translation.keyOwnedByAnotherContext",
            Map.of(
                "en",
                "Two modules declare the same translation key.",
                "pl",
                "Dwa moduły deklarują ten sam klucz tłumaczenia."
            )
        );
        defaults.put(
            "error.translation.invalidPattern",
            Map.of(
                "en",
                "This text is not a valid message pattern: {reason}",
                "pl",
                "Ten tekst nie jest poprawnym wzorcem wiadomości: {reason}"
            )
        );
        defaults.put(
            "error.translation.placeholderMismatch",
            Map.of(
                "en",
                "This text must use the placeholders {expected}, but it uses {actual}.",
                "pl",
                "Ten tekst musi używać symboli {expected}, a używa {actual}."
            )
        );
        defaults.put(
            "error.translation.importUnreadable",
            Map.of(
                "en",
                "That file could not be read as a spreadsheet.",
                "pl",
                "Nie udało się odczytać tego pliku jako arkusza."
            )
        );
        defaults.put(
            "error.translation.importTooLarge",
            Map.of(
                "en",
                "That spreadsheet has {rows} rows; at most {limit} are accepted.",
                "pl",
                "Ten arkusz ma {rows} wierszy; przyjmowanych jest najwyżej {limit}."
            )
        );
        defaults.put(
            "error.translation.unattributableAuthor",
            Map.of(
                "en",
                "Your token carries no username, so the change cannot be attributed.",
                "pl",
                "Token nie zawiera nazwy użytkownika, więc nie można przypisać autora zmiany."
            )
        );

        defaults.put("translation.export.column.key", Map.of("en", "Key", "pl", "Klucz"));
        defaults.put("translation.export.column.context", Map.of("en", "Module", "pl", "Moduł"));

        defaults.put(
            "permission.translations.read",
            Map.of(
                "en",
                "See the text the application ships and what has been edited.",
                "pl",
                "Podgląd tekstów aplikacji i tego, co zostało zmienione."
            )
        );
        defaults.put(
            "permission.translations.edit",
            Map.of(
                "en",
                "Change the text the application shows.",
                "pl",
                "Zmiana tekstów wyświetlanych przez aplikację."
            )
        );

        return Map.copyOf(defaults);
    }
}
