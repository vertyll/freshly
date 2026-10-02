package com.vertyll.freshly.auth.infrastructure.config;

import java.util.List;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.i18n.TranslationMigration;
import com.vertyll.freshly.lang.i18n.TranslationMigrationOperations;

@Component
public class HostedSignInTranslationMigration implements TranslationMigration {
    private static final List<String> RETIRED = List.of(
        "error.auth.currentPasswordIncorrect",
        "error.auth.emailAlreadyExists",
        "error.auth.invalidCredentials",
        "error.auth.tokenExpired",
        "error.auth.tokenFormatInvalid",
        "error.auth.tokenInvalid",
        "error.auth.tokenTypeInvalid",
        "error.auth.userNotFound",
        "error.auth.usernameAlreadyExists",
        "error.auth.weakPassword",
        "error.notification.alreadySent",
        "error.notification.deliveryFailed",
        "error.notification.invalidRecipient",
        "error.notification.templateRenderFailed",
        "email.linkHint",
        "email.passwordReset.button",
        "email.passwordReset.expiry",
        "email.passwordReset.footer",
        "email.passwordReset.greeting",
        "email.passwordReset.ignore",
        "email.passwordReset.intro",
        "email.passwordReset.title",
        "email.userRegistered.footer",
        "email.userRegistered.greeting",
        "email.userRegistered.intro",
        "email.userRegistered.nextSteps",
        "email.userRegistered.support",
        "email.userRegistered.title",
        "email.verification.button",
        "email.verification.expiry",
        "email.verification.footer",
        "email.verification.greeting",
        "email.verification.ignore",
        "email.verification.intro",
        "email.verification.title"
    );

    @Override
    public String id() {
        return "2026-10-auth-hosted-sign-in";
    }

    @Override
    public String context() {
        return "auth";
    }

    @Override
    public void apply(TranslationMigrationOperations operations) {
        operations.rename("error.auth.refreshTokenInvalid", "error.auth.sessionExpired");
        RETIRED.forEach(operations::retire);
    }
}
