package com.vertyll.freshly.useraccess.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.i18n.TranslationCatalogue;

@Component
public class UserAccessTranslationCatalogue implements TranslationCatalogue {
    @Override
    public String context() {
        return "useraccess";
    }

    @Override
    public Map<String, Map<String, String>> defaults() {
        Map<String, Map<String, String>> defaults = new LinkedHashMap<>();

        defaults.put(
            "error.user.alreadyActive",
            Map.of("en", "User is already active", "pl", "Użytkownik jest już aktywny")
        );
        defaults.put(
            "error.user.alreadyExists",
            Map.of("en", "User already exists", "pl", "Użytkownik o takich parametrach już istnieje")
        );
        defaults.put(
            "error.user.alreadyInactive",
            Map.of("en", "User is already inactive", "pl", "Użytkownik jest już nieaktywny")
        );
        defaults.put("error.user.notFound", Map.of("en", "User not found", "pl", "Nie znaleziono użytkownika"));
        defaults.put(
            "error.user.rolesEmpty",
            Map.of("en", "User must have at least one role", "pl", "Użytkownik musi posiadać przynajmniej jedną rolę")
        );
        defaults.put(
            "error.user.selfDeactivation",
            Map.of("en", "Cannot deactivate your own account", "pl", "Nie można dezaktywować własnego konta")
        );

        defaults.put(
            "permission.users.read",
            Map.of("en", "See the list of users and their details.", "pl", "Podgląd listy użytkowników i ich danych.")
        );
        defaults
            .put("permission.users.create", Map.of("en", "Add a user record.", "pl", "Dodawanie rekordu użytkownika."));
        defaults.put(
            "permission.users.update",
            Map.of("en", "Change a user's details.", "pl", "Zmiana danych użytkownika.")
        );
        defaults.put(
            "permission.users.delete",
            Map.of("en", "Remove a user record.", "pl", "Usuwanie rekordu użytkownika.")
        );
        defaults.put(
            "permission.users.activate",
            Map.of("en", "Let a user sign in again.", "pl", "Przywracanie użytkownikowi możliwości logowania.")
        );
        defaults.put(
            "permission.users.deactivate",
            Map.of("en", "Stop a user signing in.", "pl", "Odbieranie użytkownikowi możliwości logowania.")
        );
        defaults.put(
            "permission.users.manageRoles",
            Map.of(
                "en",
                "Assign roles, which is what decides everything else a person may do.",
                "pl",
                "Przypisywanie ról, od których zależy wszystko inne, co dana osoba może zrobić."
            )
        );
        defaults.put(
            "error.user.unknownRoles",
            Map.of(
                "en",
                "No such role in the identity provider: {roles}",
                "pl",
                "Dostawca tożsamości nie zna takiej roli: {roles}"
            )
        );

        return Map.copyOf(defaults);
    }
}
