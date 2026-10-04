package com.vertyll.freshly.auth.infrastructure.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.i18n.TranslationMigration;
import com.vertyll.freshly.lang.i18n.TranslationMigrationOperations;

@Component
public class OAuth2ClientTranslationMigration implements TranslationMigration {
    private static final List<String> RETIRED =
            List.of("error.auth.identityProviderUnavailable", "error.auth.sessionExpired", "error.auth.signInRejected");

    @Override
    public String id() {
        return "2026-10-auth-oauth2-client";
    }

    @Override
    public String context() {
        return "auth";
    }

    @Override
    public void apply(TranslationMigrationOperations operations) {
        RETIRED.forEach(operations::retire);
    }
}
