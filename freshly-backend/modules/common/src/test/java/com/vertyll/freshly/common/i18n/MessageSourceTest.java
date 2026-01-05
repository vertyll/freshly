package com.vertyll.freshly.common.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSourceTest {

    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setDefaultLocale(Locale.ENGLISH);
        this.messageSource = source;
    }

    @ParameterizedTest
    @CsvSource({
            "en-US, success.user.created, User created successfully",
            "pl, success.user.created, Użytkownik utworzony pomyślnie",
            "en-US, error.user.notFound, User not found",
            "pl, error.user.notFound, Nie znaleziono użytkownika",
            "de, success.user.created, User created successfully"
    })
    @DisplayName("Should load messages for different locales and keys")
    void shouldLoadMessagesForLocaleAndKey(String localeTag, String messageKey, String expectedMessage) {
        // Given
        Locale locale = Locale.forLanguageTag(localeTag);
        LocaleContextHolder.setLocale(locale);

        // When
        String message = messageSource.getMessage(messageKey, null, locale);

        // Then
        assertThat(message).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should return key when message not found")
    void shouldReturnKeyAsMessage_whenKeyNotFound() {
        // Given
        Locale locale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(locale);
        String nonExistentKey = "nonexistent.message.key";

        // When
        String message = messageSource.getMessage(
                nonExistentKey,
                null,
                nonExistentKey,
                locale
        );

        // Then
        assertThat(message).isEqualTo(nonExistentKey);
    }
}
