package com.vertyll.freshly.airquality.api;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import com.vertyll.freshly.airquality.domain.exception.AirQualityDataNotFoundException;
import com.vertyll.freshly.airquality.domain.exception.GiosApiException;
import com.vertyll.freshly.airquality.domain.exception.InvalidDateRangeException;
import com.vertyll.freshly.airquality.domain.exception.StationNotFoundException;

@ExtendWith(MockitoExtension.class)
class AirQualityControllerAdviceTest {

    @Mock
    @SuppressWarnings("NullAway.Init")
    private MessageSource messageSource;

    private AirQualityControllerAdvice controllerAdvice;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        controllerAdvice = new AirQualityControllerAdvice(messageSource);
    }

    @Test
    @DisplayName("Should handle StationNotFoundException")
    void shouldHandleStationNotFoundException() {
        // Given
        int stationId = 123;
        StationNotFoundException exception = new StationNotFoundException(stationId);
        String errorMessage = "Station not found";

        when(messageSource.getMessage(
                        eq("error.airquality.stationNotFound"), isNull(), any(Locale.class)))
                .thenReturn(errorMessage);

        // When
        ProblemDetail result = controllerAdvice.handleStationNotFound(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).isEqualTo(errorMessage);
        verify(messageSource)
                .getMessage(eq("error.airquality.stationNotFound"), isNull(), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle AirQualityDataNotFoundException")
    void shouldHandleAirQualityDataNotFoundException() {
        // Given
        int stationId = 123;
        AirQualityDataNotFoundException exception = new AirQualityDataNotFoundException(stationId);
        String errorMessage = "Air quality data not found";

        when(messageSource.getMessage(
                        eq("error.airquality.dataNotFound"), isNull(), any(Locale.class)))
                .thenReturn(errorMessage);

        // When
        ProblemDetail result = controllerAdvice.handleAirQualityDataNotFound(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).isEqualTo(errorMessage);
        verify(messageSource)
                .getMessage(eq("error.airquality.dataNotFound"), isNull(), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle InvalidDateRangeException")
    void shouldHandleInvalidDateRangeException() {
        // Given
        InvalidDateRangeException exception = new InvalidDateRangeException("Invalid date range");
        String errorMessage = "Invalid date range provided";

        when(messageSource.getMessage(
                        eq("error.airquality.invalidDateRange"), isNull(), any(Locale.class)))
                .thenReturn(errorMessage);

        // When
        ProblemDetail result = controllerAdvice.handleInvalidDateRange(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getDetail()).isEqualTo(errorMessage);
        verify(messageSource)
                .getMessage(eq("error.airquality.invalidDateRange"), isNull(), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle GiosApiException")
    void shouldHandleGiosApiException() {
        // Given
        GiosApiException exception = new GiosApiException("GIOS API error");
        String errorMessage = "External service error";

        when(messageSource.getMessage(
                        eq("error.airquality.giosApiUnavailable"), isNull(), any(Locale.class)))
                .thenReturn(errorMessage);

        // When
        ProblemDetail result = controllerAdvice.handleGiosApiException(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(result.getDetail()).isEqualTo(errorMessage);
        verify(messageSource)
                .getMessage(eq("error.airquality.giosApiUnavailable"), isNull(), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle AirQualityDataNotFoundException with custom message")
    void shouldHandleAirQualityDataNotFoundExceptionWithCustomMessage() {
        // Given
        String customMessage = "No data available for this station";
        AirQualityDataNotFoundException exception =
                new AirQualityDataNotFoundException(customMessage);
        String errorMessage = "Air quality data not found";

        when(messageSource.getMessage(
                        eq("error.airquality.dataNotFound"), isNull(), any(Locale.class)))
                .thenReturn(errorMessage);

        // When
        ProblemDetail result = controllerAdvice.handleAirQualityDataNotFound(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).isEqualTo(errorMessage);
    }
}
