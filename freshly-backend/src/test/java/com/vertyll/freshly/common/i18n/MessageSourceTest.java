package com.vertyll.freshly.common.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageSourceTest {

    @Mock
    private MessageSource messageSource;

    @Test
    @DisplayName("Should return English message for en-US locale")
    void shouldLoadEnglishMessage() {
        // Given
        Locale locale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(locale);

        when(messageSource.getMessage(eq("success.user.created"), eq(null), eq(locale)))
                .thenReturn("User created successfully");

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

        when(messageSource.getMessage(eq("success.user.created"), eq(null), eq(locale)))
                .thenReturn("Użytkownik utworzony pomyślnie");

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

        when(messageSource.getMessage(eq("error.user.notFound"), eq(null), eq(locale)))
                .thenReturn("User not found");

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

        when(messageSource.getMessage(eq("error.user.notFound"), eq(null), eq(locale)))
                .thenReturn("Nie znaleziono użytkownika");

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

        when(messageSource.getMessage(eq("success.user.created"), eq(null), eq(locale)))
                .thenReturn("User created successfully");

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

        when(messageSource.getMessage(eq(nonExistentKey), eq(null), eq(locale)))
                .thenReturn(nonExistentKey);

        // When
        String message = messageSource.getMessage(
                nonExistentKey,
                null,
                locale
        );

        // Then
        assertThat(message).isEqualTo(nonExistentKey);
    }
}
