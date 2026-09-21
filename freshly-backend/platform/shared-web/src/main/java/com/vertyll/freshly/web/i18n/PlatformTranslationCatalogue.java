package com.vertyll.freshly.web.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.i18n.TranslationCatalogue;

/**
 * The default text this context ships.
 *
 * <p>
 * These are <em>defaults</em>. The {@code translation} context stores them and
 * overwrites them on every start-up, so improving a sentence here reaches everyone who has
 * not overridden that key. An administrator's override is kept separately and is never
 * touched by a redeploy.
 */
@Component
public class PlatformTranslationCatalogue implements TranslationCatalogue {

    @Override
    public String context() {
        return "platform";
    }

    @Override
    public Map<String, Map<String, String>> defaults() {
        Map<String, Map<String, String>> defaults = new LinkedHashMap<>();

        defaults.put(
            "common.copyright",
            Map.of("en", "© 2026 Freshly. All rights reserved.", "pl", "© 2026 Freshly. Wszelkie prawa zastrzeżone")
        );
        defaults.put("common.freshly", Map.of("en", "Freshly", "pl", "Freshly"));
        defaults.put(
            "error.common.unexpectedError",
            Map.of("en", "Something went wrong on our side.", "pl", "Coś poszło nie tak po naszej stronie.")
        );
        defaults.put(
            "error.common.validationFailed",
            Map.of(
                "en",
                "The request could not be accepted as sent.",
                "pl",
                "Żądanie nie może zostać przyjęte w tej postaci."
            )
        );
        defaults.put(
            "error.common.malformedIfMatch",
            Map.of(
                "en",
                "The If-Match header must carry one version this API issued, not {value}.",
                "pl",
                "Nagłówek If-Match musi zawierać jedną wersję wydaną przez to API, a nie {value}."
            )
        );
        defaults.put(
            "error.common.versionMismatch",
            Map.of(
                "en",
                "The resource was modified by someone else. Reload it and try again.",
                "pl",
                "Zasób został w międzyczasie zmieniony. Odśwież i spróbuj ponownie."
            )
        );
        defaults.put(
            "error.common.concurrentModification",
            Map.of(
                "en",
                "Somebody else changed this at the same time. Reload it and try again.",
                "pl",
                "Ktoś zmienił to w tej samej chwili. Odśwież i spróbuj ponownie."
            )
        );
        defaults.put("error.common.alreadyExists", Map.of("en", "That already exists.", "pl", "To już istnieje."));
        defaults.put(
            "error.common.uploadTooLarge",
            Map.of("en", "That file is too large.", "pl", "Ten plik jest za duży.")
        );
        defaults.put(
            "error.security.accessDenied",
            Map.of("en", "You do not have permission to do that.", "pl", "Nie masz uprawnień do tej operacji.")
        );
        defaults.put(
            "error.security.unauthenticated",
            Map.of("en", "Authentication is required.", "pl", "Wymagane uwierzytelnienie.")
        );

        defaults.put("validation.required", Map.of("en", "This field is required.", "pl", "To pole jest wymagane."));
        defaults.put(
            "validation.size",
            Map.of("en", "Must be between {min} and {max} characters.", "pl", "Musi mieć od {min} do {max} znaków.")
        );
        defaults.put(
            "validation.min",
            Map.of("en", "Must be at least {value}.", "pl", "Musi wynosić co najmniej {value}.")
        );
        defaults
            .put("validation.max", Map.of("en", "Must be at most {value}.", "pl", "Może wynosić najwyżej {value}."));
        defaults
            .put("validation.positive", Map.of("en", "Must be greater than zero.", "pl", "Musi być większe od zera."));
        defaults.put("validation.positiveOrZero", Map.of("en", "Cannot be negative.", "pl", "Nie może być ujemne."));
        defaults
            .put("validation.negative", Map.of("en", "Must be less than zero.", "pl", "Musi być mniejsze od zera."));
        defaults.put(
            "validation.email",
            Map.of("en", "This is not a valid email address.", "pl", "To nie jest poprawny adres e-mail.")
        );
        defaults.put(
            "validation.pattern",
            Map.of("en", "This value is not in the expected format.", "pl", "Ta wartość nie ma oczekiwanego formatu.")
        );
        defaults
            .put("validation.past", Map.of("en", "Must be a date in the past.", "pl", "Musi być datą z przeszłości."));
        defaults.put(
            "validation.future",
            Map.of("en", "Must be a date in the future.", "pl", "Musi być datą w przyszłości.")
        );
        defaults.put(
            "validation.mustBeTrue",
            Map.of("en", "This must be accepted.", "pl", "To musi zostać zaakceptowane.")
        );
        defaults
            .put("validation.mustBeFalse", Map.of("en", "This must not be set.", "pl", "To nie może być ustawione."));
        defaults.put(
            "validation.invalid",
            Map.of("en", "This value is not valid.", "pl", "Ta wartość jest nieprawidłowa.")
        );
        defaults.put(
            "validation.typeMismatch",
            Map.of(
                "en",
                "The parameter {parameter} could not be read.",
                "pl",
                "Nie udało się odczytać parametru {parameter}."
            )
        );
        defaults.put(
            "validation.unreadableBody",
            Map.of("en", "The request body could not be read.", "pl", "Nie udało się odczytać treści żądania.")
        );

        return Map.copyOf(defaults);
    }
}
