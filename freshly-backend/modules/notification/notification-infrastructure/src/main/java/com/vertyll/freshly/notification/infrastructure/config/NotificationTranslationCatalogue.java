package com.vertyll.freshly.notification.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.i18n.TranslationCatalogue;

@Component
public class NotificationTranslationCatalogue implements TranslationCatalogue {
    @Override
    public String context() {
        return "notification";
    }

    @Override
    public Map<String, Map<String, String>> defaults() {
        Map<String, Map<String, String>> defaults = new LinkedHashMap<>();

        defaults.put(
            "email.linkHint",
            Map.of(
                "en",
                "Or copy and paste this link into your browser:",
                "pl",
                "Albo skopiuj ten link do przeglądarki:"
            )
        );
        defaults.put("email.passwordReset.button", Map.of("en", "Reset Password", "pl", "Zresetuj hasło"));
        defaults.put(
            "email.passwordReset.expiry",
            Map.of(
                "en",
                "This password reset link will expire in 1 hour.",
                "pl",
                "Link do resetowania hasła wygaśnie za godzinę."
            )
        );
        defaults.put(
            "email.passwordReset.footer",
            Map.of(
                "en",
                "This is an automated message, please do not reply to this email.",
                "pl",
                "To jest wiadomość wygenerowana automatycznie, prosimy na nią nie odpowiadać."
            )
        );
        defaults.put("email.passwordReset.greeting", Map.of("en", "Hi", "pl", "Witaj,"));
        defaults.put(
            "email.passwordReset.ignore",
            Map.of(
                "en",
                "If you didn't request a password reset, you can safely ignore this email. Your password will remain unchanged.",
                "pl",
                "Jeśli nie prosiłeś o zmianę hasła, zignoruj tę wiadomość. Twoje obecne hasło pozostanie bezpieczne."
            )
        );
        defaults.put(
            "email.passwordReset.intro",
            Map.of(
                "en",
                "We received a request to reset your password for your Freshly account. Click the button below to set a new password.",
                "pl",
                "Otrzymaliśmy prośbę o zresetowanie hasła do Twojego konta w serwisie Freshly. Kliknij poniższy przycisk, aby ustawić nowe hasło."
            )
        );
        defaults.put("email.passwordReset.title", Map.of("en", "Reset Your Password", "pl", "Zresetuj swoje hasło"));
        defaults.put(
            "email.userRegistered.footer",
            Map.of(
                "en",
                "This is an automated message, please do not reply to this email.",
                "pl",
                "To jest wiadomość wygenerowana automatycznie, prosimy na nią nie odpowiadać."
            )
        );
        defaults.put("email.userRegistered.greeting", Map.of("en", "Hi", "pl", "Witaj,"));
        defaults.put(
            "email.userRegistered.intro",
            Map.of(
                "en",
                "Your account has been successfully created! We're excited to have you on board.",
                "pl",
                "Twoje konto zostało pomyślnie utworzone. Cieszymy się, że wspólnie z nami będziesz dbać o jakość powietrza w swojej okolicy!"
            )
        );
        defaults.put("email.userRegistered.nextSteps", Map.of("en", "What's Next?", "pl", "Co teraz?"));
        defaults.put(
            "email.userRegistered.step1",
            Map.of("en", "Verify your email address", "pl", "Potwierdź swój adres email")
        );
        defaults.put(
            "email.userRegistered.step2",
            Map.of("en", "Complete your profile", "pl", "Uzupełnij informacje w profilu")
        );
        defaults.put(
            "email.userRegistered.step3",
            Map.of("en", "Start monitoring air quality in your area", "pl", "Sprawdź jakość powietrza w Twoim regionie")
        );
        defaults.put(
            "email.userRegistered.support",
            Map.of(
                "en",
                "If you have any questions, feel free to reach out to our support team.",
                "pl",
                "Jeśli masz pytania, skontaktuj się z naszym zespołem wsparcia."
            )
        );
        defaults
            .put("email.userRegistered.title", Map.of("en", "Welcome to Freshly!", "pl", "Witaj w zespole Freshly!"));
        defaults.put("email.verification.button", Map.of("en", "Verify Email", "pl", "Potwierdź adres email"));
        defaults.put(
            "email.verification.expiry",
            Map.of(
                "en",
                "This verification link will expire in 24 hours.",
                "pl",
                "Ten link weryfikacyjny wygaśnie za 24 godziny."
            )
        );
        defaults.put(
            "email.verification.footer",
            Map.of(
                "en",
                "This is an automated message, please do not reply to this email.",
                "pl",
                "To jest wiadomość wygenerowana automatycznie, prosimy na nią nie odpowiadać."
            )
        );
        defaults.put("email.verification.greeting", Map.of("en", "Hi", "pl", "Witaj,"));
        defaults.put(
            "email.verification.ignore",
            Map.of(
                "en",
                "If you didn't create an account, you can safely ignore this email.",
                "pl",
                "Jeśli to nie Ty zakładałeś konto, możesz bezpiecznie zignorować tę wiadomość."
            )
        );
        defaults.put(
            "email.verification.intro",
            Map.of(
                "en",
                "Thank you for registering with Freshly! To complete your registration and start using our air quality monitoring service, please verify your email address.",
                "pl",
                "Dziękujemy za rejestrację w Freshly! Aby korzystać z pełnej funkcjonalności serwisu monitoringu jakości powietrza, potwierdź swój adres email."
            )
        );
        defaults.put(
            "email.verification.title",
            Map.of("en", "Verify Your Email Address", "pl", "Weryfikacja adresu email")
        );
        defaults.put(
            "error.notification.alreadySent",
            Map.of("en", "That message has already been sent.", "pl", "Ta wiadomość została już wysłana.")
        );
        defaults.put(
            "error.notification.deliveryFailed",
            Map.of(
                "en",
                "The message could not be sent. Try again shortly.",
                "pl",
                "Nie udało się wysłać wiadomości. Spróbuj za chwilę."
            )
        );
        defaults.put(
            "error.notification.invalidRecipient",
            Map.of("en", "That is not a valid email address.", "pl", "To nie jest poprawny adres e-mail.")
        );
        defaults.put(
            "error.notification.templateRenderFailed",
            Map.of("en", "The message could not be prepared.", "pl", "Nie udało się przygotować wiadomości.")
        );

        return Map.copyOf(defaults);
    }
}
