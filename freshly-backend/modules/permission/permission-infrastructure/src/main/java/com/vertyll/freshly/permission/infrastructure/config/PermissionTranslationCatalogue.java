package com.vertyll.freshly.permission.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.i18n.TranslationCatalogue;

@Component
public class PermissionTranslationCatalogue implements TranslationCatalogue {
    @Override
    public String context() {
        return "permission";
    }

    @Override
    public Map<String, Map<String, String>> defaults() {
        Map<String, Map<String, String>> defaults = new LinkedHashMap<>();

        defaults.put(
            "error.permission.blankRole",
            Map.of("en", "A role name is required.", "pl", "Nazwa roli jest wymagana.")
        );
        defaults.put(
            "error.permission.unknownPermission",
            Map.of("en", "No module declares that permission.", "pl", "Żaden moduł nie deklaruje takiego uprawnienia.")
        );

        defaults.put(
            "permission.permissions.read",
            Map.of("en", "See which role holds which permission.", "pl", "Podgląd, która rola ma które uprawnienie.")
        );
        defaults.put(
            "permission.permissions.manage",
            Map.of(
                "en",
                "Change what a role may do, including taking permissions away.",
                "pl",
                "Zmiana tego, co może robić dana rola, łącznie z odbieraniem uprawnień."
            )
        );
        defaults.put(
            "error.permission.roleNotFound",
            Map.of("en", "That role holds no permissions here.", "pl", "Ta rola nie ma tu żadnych uprawnień.")
        );

        return Map.copyOf(defaults);
    }
}
