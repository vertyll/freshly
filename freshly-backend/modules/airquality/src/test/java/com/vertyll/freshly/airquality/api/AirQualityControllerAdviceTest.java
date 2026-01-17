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

    private static final String ERROR_STATION_NOT_FOUND_MSG_KEY =
            "error.airquality.stationNotFound";
    private static final String ERROR_DATA_NOT_FOUND_MSG_KEY = "error.airquality.dataNotFound";
    private static final String ERROR_INVALID_DATE_RANGE_MSG_KEY =
            "error.airquality.invalidDateRange";
    private static final String ERROR_GIOS_API_UNAVAILABLE_MSG_KEY =
            "error.airquality.giosApiUnavailable";

    private static final int TEST_STATION_ID = 123;

    private static final String STATION_NOT_FOUND = "Station not found";
    private static final String AIR_QUALITY_DATA_NOT_FOUND = "Air quality data not found";
    private static final String INVALID_DATE_RANGE = "Invalid date range";
    private static final String INVALID_DATE_RANGE_PROVIDED = "Invalid date range provided";
    private static final String GIOS_API_ERROR = "GIOS API error";
    private static final String EXTERNAL_SERVICE_ERROR = "External service error";
    private static final String NO_DATA_AVAILABLE = "No data available for this station";

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
        StationNotFoundException exception = new StationNotFoundException(TEST_STATION_ID);

        when(messageSource.getMessage(
                        eq(ERROR_STATION_NOT_FOUND_MSG_KEY), isNull(), any(Locale.class)))
                .thenReturn(STATION_NOT_FOUND);

        // When
        ProblemDetail result = controllerAdvice.handleStationNotFound(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).isEqualTo(STATION_NOT_FOUND);
        verify(messageSource)
                .getMessage(eq(ERROR_STATION_NOT_FOUND_MSG_KEY), isNull(), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle AirQualityDataNotFoundException")
    void shouldHandleAirQualityDataNotFoundException() {
        // Given
        AirQualityDataNotFoundException exception =
                new AirQualityDataNotFoundException(TEST_STATION_ID);

        when(messageSource.getMessage(
                        eq(ERROR_DATA_NOT_FOUND_MSG_KEY), isNull(), any(Locale.class)))
                .thenReturn(AIR_QUALITY_DATA_NOT_FOUND);

        // When
        ProblemDetail result = controllerAdvice.handleAirQualityDataNotFound(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).isEqualTo(AIR_QUALITY_DATA_NOT_FOUND);
        verify(messageSource)
                .getMessage(eq(ERROR_DATA_NOT_FOUND_MSG_KEY), isNull(), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle InvalidDateRangeException")
    void shouldHandleInvalidDateRangeException() {
        // Given
        InvalidDateRangeException exception = new InvalidDateRangeException(INVALID_DATE_RANGE);

        when(messageSource.getMessage(
                        eq(ERROR_INVALID_DATE_RANGE_MSG_KEY), isNull(), any(Locale.class)))
                .thenReturn(INVALID_DATE_RANGE_PROVIDED);

        // When
        ProblemDetail result = controllerAdvice.handleInvalidDateRange(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getDetail()).isEqualTo(INVALID_DATE_RANGE_PROVIDED);
        verify(messageSource)
                .getMessage(eq(ERROR_INVALID_DATE_RANGE_MSG_KEY), isNull(), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle GiosApiException")
    void shouldHandleGiosApiException() {
        // Given
        GiosApiException exception = new GiosApiException(GIOS_API_ERROR);

        when(messageSource.getMessage(
                        eq(ERROR_GIOS_API_UNAVAILABLE_MSG_KEY), isNull(), any(Locale.class)))
                .thenReturn(EXTERNAL_SERVICE_ERROR);

        // When
        ProblemDetail result = controllerAdvice.handleGiosApiException(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(result.getDetail()).isEqualTo(EXTERNAL_SERVICE_ERROR);
        verify(messageSource)
                .getMessage(eq(ERROR_GIOS_API_UNAVAILABLE_MSG_KEY), isNull(), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle AirQualityDataNotFoundException with custom message")
    void shouldHandleAirQualityDataNotFoundExceptionWithCustomMessage() {
        // Given
        AirQualityDataNotFoundException exception =
                new AirQualityDataNotFoundException(NO_DATA_AVAILABLE);

        when(messageSource.getMessage(
                        eq(ERROR_DATA_NOT_FOUND_MSG_KEY), isNull(), any(Locale.class)))
                .thenReturn(AIR_QUALITY_DATA_NOT_FOUND);

        // When
        ProblemDetail result = controllerAdvice.handleAirQualityDataNotFound(exception);

        // Then
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).isEqualTo(AIR_QUALITY_DATA_NOT_FOUND);
    }
}
