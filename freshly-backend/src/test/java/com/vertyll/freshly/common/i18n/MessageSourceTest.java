package com.vertyll.freshly.common.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSourceTest {

    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(Locale.ENGLISH);
        this.messageSource = messageSource;
    }

    @Test
    @DisplayName("Should return English message for en-US locale")
    void shouldLoadEnglishMessage() {
        // Given
        Locale locale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(locale);

        // When
        String message = messageSource.getMessage(
                "success.user.created",
                null,
                locale
        );

        // Then
        assertThat(message).isEqualTo("User created successfully");
    }

    @Test
    @DisplayName("Should return Polish message for pl locale")
    void shouldLoadPolishMessage() {
        // Given
        Locale locale = Locale.forLanguageTag("pl");
        LocaleContextHolder.setLocale(locale);

        // When
        String message = messageSource.getMessage(
                "success.user.created",
                null,
                locale
        );

        // Then
        assertThat(message).isEqualTo("Użytkownik utworzony pomyślnie");
    }

    @Test
    @DisplayName("Should return English error message")
    void shouldLoadErrorMessage_English() {
        // Given
        Locale locale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(locale);

        // When
        String message = messageSource.getMessage(
                "error.user.notFound",
                null,
                locale
        );

        // Then
        assertThat(message).isEqualTo("User not found");
    }

    @Test
    @DisplayName("Should return Polish error message")
    void shouldLoadErrorMessage_Polish() {
        // Given
        Locale locale = Locale.forLanguageTag("pl");
        LocaleContextHolder.setLocale(locale);

        // When
        String message = messageSource.getMessage(
                "error.user.notFound",
                null,
                locale
        );

        // Then
        assertThat(message).isEqualTo("Nie znaleziono użytkownika");
    }

    @Test
    @DisplayName("Should fallback to English when locale not supported")
    void shouldFallbackToEnglish_whenLocaleNotSupported() {
        // Given
        Locale locale = Locale.forLanguageTag("de");
        LocaleContextHolder.setLocale(locale);

        // When
        String message = messageSource.getMessage(
                "success.user.created",
                null,
                locale
        );

        // Then
        assertThat(message).isEqualTo("User created successfully");
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
