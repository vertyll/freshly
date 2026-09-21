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
            "error.auth.currentPasswordIncorrect",
            Map.of("en", "The current password is incorrect.", "pl", "Obecne hasło jest nieprawidłowe.")
        );
        defaults.put(
            "error.auth.emailAlreadyExists",
            Map.of(
                "en",
                "An account already exists for that email address.",
                "pl",
                "Konto dla tego adresu e-mail już istnieje."
            )
        );
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
            "error.auth.invalidCredentials",
            Map.of("en", "Incorrect username or password.", "pl", "Nieprawidłowa nazwa użytkownika lub hasło.")
        );
        defaults.put(
            "error.auth.refreshTokenInvalid",
            Map.of("en", "Your session has ended. Please sign in again.", "pl", "Sesja wygasła. Zaloguj się ponownie.")
        );
        defaults.put(
            "error.auth.tokenExpired",
            Map.of("en", "This link has expired. Request a new one.", "pl", "Ten link wygasł. Poproś o nowy.")
        );
        defaults.put(
            "error.auth.tokenFormatInvalid",
            Map.of("en", "Invalid token format", "pl", "Nieprawidłowy format tokena")
        );
        defaults.put(
            "error.auth.tokenInvalid",
            Map.of("en", "This link is not valid.", "pl", "Ten link jest nieprawidłowy.")
        );
        defaults
            .put("error.auth.tokenTypeInvalid", Map.of("en", "Invalid token type", "pl", "Nieprawidłowy typ tokena"));
        defaults.put("error.auth.userNotFound", Map.of("en", "No such user.", "pl", "Nie ma takiego użytkownika."));
        defaults.put(
            "error.auth.usernameAlreadyExists",
            Map.of("en", "That username is already taken.", "pl", "Ta nazwa użytkownika jest już zajęta.")
        );
        defaults.put(
            "error.auth.weakPassword",
            Map.of("en", "That password does not meet the requirements.", "pl", "Hasło nie spełnia wymagań.")
        );

        return Map.copyOf(defaults);
    }
}
