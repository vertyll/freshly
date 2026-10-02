package com.vertyll.freshly.auth.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.i18n.TranslationCatalogue;

@Component
public class AuthTranslationCatalogue implements TranslationCatalogue {
    @Override
    public String context() {
        return "auth";
    }

    @Override
    public Map<String, Map<String, String>> defaults() {
        Map<String, Map<String, String>> defaults = new LinkedHashMap<>();

        defaults.put(
            "error.auth.identityProviderUnavailable",
            Map.of(
                "en",
                "The sign-in service is unavailable. Try again shortly.",
                "pl",
                "Usługa logowania jest niedostępna. Spróbuj za chwilę."
            )
        );
        defaults.put(
            "error.auth.sessionExpired",
            Map.of("en", "Your session has ended. Please sign in again.", "pl", "Sesja wygasła. Zaloguj się ponownie.")
        );
        defaults.put(
            "error.auth.signInRejected",
            Map.of(
                "en",
                "The sign-in could not be completed. Please try again.",
                "pl",
                "Nie udało się dokończyć logowania. Spróbuj ponownie."
            )
        );

        return Map.copyOf(defaults);
    }
}
