package com.vertyll.freshly.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiResponseTest {

    @Mock
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

        when(messageSource.getMessage(eq(messageKey), eq(null), eq(locale)))
                .thenReturn(localizedMessage);

        // When
        ResponseEntity<ApiResponse<String>> response = ApiResponse.buildResponse(
                data,
                messageKey,
                messageSource,
                HttpStatus.CREATED
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(data);
        assertThat(response.getBody().getMessage()).isEqualTo(localizedMessage);
        assertThat(response.getBody().getTimestamp()).isNotNull();
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

        when(messageSource.getMessage(eq(messageKey), eq(null), eq(locale)))
                .thenReturn(localizedMessage);

        // When
        ResponseEntity<ApiResponse<String>> response = ApiResponse.buildResponse(
                data,
                messageKey,
                messageSource,
                HttpStatus.CREATED
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(data);
        assertThat(response.getBody().getMessage()).isEqualTo(localizedMessage);
    }

    @Test
    @DisplayName("Should build response with localized message and null data")
    void shouldBuildResponseWithLocalizedMessageAndNullData() {
        // Given
        String messageKey = "success.operation.completed";
        String localizedMessage = "Operation completed";
        Locale locale = Locale.forLanguageTag("en-US");
        LocaleContextHolder.setLocale(locale);

        when(messageSource.getMessage(eq(messageKey), eq(null), eq(locale)))
                .thenReturn(localizedMessage);

        // When
        ResponseEntity<ApiResponse<Void>> response = ApiResponse.buildResponse(
                null,
                messageKey,
                messageSource,
                HttpStatus.OK
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
        assertThat(response.getBody().getMessage()).isEqualTo(localizedMessage);
    }
}
