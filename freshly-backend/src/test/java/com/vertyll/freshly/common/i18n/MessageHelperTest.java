package com.vertyll.freshly.common.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageHelperTest {

    @Mock
    private MessageSource messageSource;

    private MessageHelper messageHelper;

    @BeforeEach
    void setUp() {
        messageHelper = new MessageHelper(messageSource);
    }

    @Test
    @DisplayName("Should get message for current locale without parameters")
    void shouldGetMessageForCurrentLocale() {
        // Given
        String key = "success.user.created";
        String expectedMessage = "User created successfully";
        Locale currentLocale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(currentLocale);

        when(messageSource.getMessage(key, null, currentLocale))
                .thenReturn(expectedMessage);

        // When
        String message = messageHelper.getMessage(key);

        // Then
        assertThat(message).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should get message with parameters for current locale")
    void shouldGetMessageWithParameters() {
        // Given
        String key = "success.user.welcomed";
        Object[] args = {"John", "Doe"};
        String expectedMessage = "Welcome John Doe";
        Locale currentLocale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(currentLocale);

        when(messageSource.getMessage(key, args, currentLocale))
                .thenReturn(expectedMessage);

        // When
        String message = messageHelper.getMessage(key, args);

        // Then
        assertThat(message).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should get message for specific locale without parameters")
    void shouldGetMessageForSpecificLocale() {
        // Given
        String key = "success.user.created";
        Locale specificLocale = Locale.forLanguageTag("pl");
        String expectedMessage = "Użytkownik utworzony pomyślnie";

        when(messageSource.getMessage(key, null, specificLocale))
                .thenReturn(expectedMessage);

        // When
        String message = messageHelper.getMessage(key, specificLocale);

        // Then
        assertThat(message).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should get message with parameters for specific locale")
    void shouldGetMessageWithParametersForSpecificLocale() {
        // Given
        String key = "success.user.welcomed";
        Object[] args = {"Jan", "Kowalski"};
        Locale specificLocale = Locale.forLanguageTag("pl");
        String expectedMessage = "Witaj Jan Kowalski";

        when(messageSource.getMessage(key, args, specificLocale))
                .thenReturn(expectedMessage);

        // When
        String message = messageHelper.getMessage(key, args, specificLocale);

        // Then
        assertThat(message).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should handle empty parameters array")
    void shouldHandleEmptyParametersArray() {
        // Given
        String key = "success.user.created";
        Object[] args = {};
        String expectedMessage = "User created successfully";
        Locale currentLocale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(currentLocale);

        when(messageSource.getMessage(key, args, currentLocale))
                .thenReturn(expectedMessage);

        // When
        String message = messageHelper.getMessage(key, args);

        // Then
        assertThat(message).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
        // Given
        String key = "success.user.created";
        String expectedMessage = "User created successfully";
        Locale currentLocale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(currentLocale);

        when(messageSource.getMessage(eq(key), any(), eq(currentLocale)))
                .thenReturn(expectedMessage);

        // When
        String message = messageHelper.getMessage(key, (Object[]) null);

        // Then
        assertThat(message).isEqualTo(expectedMessage);
    }
}
