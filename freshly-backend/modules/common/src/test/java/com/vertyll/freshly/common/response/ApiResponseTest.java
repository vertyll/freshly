package com.vertyll.freshly.common.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ApiResponseTest {

    @Mock
    @SuppressWarnings("NullAway.Init")
    private MessageSource messageSource;

    @Test
    @DisplayName("Should build response with localized message")
    void shouldBuildResponseWithLocalizedMessage() {
        // Given
        String data = "test data";
        String messageKey = "success.user.created";
        String localizedMessage = "User created successfully";
        Locale locale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(locale);

        when(messageSource.getMessage(messageKey, null, locale)).thenReturn(localizedMessage);

        // When
        ResponseEntity<ApiResponse<String>> response =
                ApiResponse.buildResponse(data, messageKey, messageSource, HttpStatus.CREATED);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ApiResponse<String> body = Objects.requireNonNull(response.getBody());
        assertThat(body.getData()).isEqualTo(data);
        assertThat(body.getMessage()).isEqualTo(localizedMessage);
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should build response with localized Polish message")
    void shouldBuildResponseWithLocalizedPolishMessage() {
        // Given
        String data = "dane testowe";
        String messageKey = "success.user.created";
        String localizedMessage = "Użytkownik utworzony pomyślnie";
        Locale locale = Locale.forLanguageTag("pl");
        LocaleContextHolder.setLocale(locale);

        when(messageSource.getMessage(messageKey, null, locale)).thenReturn(localizedMessage);

        // When
        ResponseEntity<ApiResponse<String>> response =
                ApiResponse.buildResponse(data, messageKey, messageSource, HttpStatus.CREATED);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ApiResponse<String> body = Objects.requireNonNull(response.getBody());
        assertThat(body.getData()).isEqualTo(data);
        assertThat(body.getMessage()).isEqualTo(localizedMessage);
    }

    @Test
    @DisplayName("Should build response with localized message and null data")
    void shouldBuildResponseWithLocalizedMessageAndNullData() {
        // Given
        String messageKey = "success.operation.completed";
        String localizedMessage = "Operation completed";
        Locale locale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(locale);

        when(messageSource.getMessage(messageKey, null, locale)).thenReturn(localizedMessage);

        // When
        ResponseEntity<ApiResponse<Void>> response =
                ApiResponse.buildResponse(null, messageKey, messageSource, HttpStatus.OK);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Void> body = Objects.requireNonNull(response.getBody());
        assertThat(body.getData()).isNull();
        assertThat(body.getMessage()).isEqualTo(localizedMessage);
    }
}
