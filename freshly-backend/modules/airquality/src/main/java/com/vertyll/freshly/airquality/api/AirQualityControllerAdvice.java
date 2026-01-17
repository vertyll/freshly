package com.vertyll.freshly.airquality.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vertyll.freshly.airquality.domain.exception.AirQualityDataNotFoundException;
import com.vertyll.freshly.airquality.domain.exception.GiosApiException;
import com.vertyll.freshly.airquality.domain.exception.InvalidDateRangeException;
import com.vertyll.freshly.airquality.domain.exception.StationNotFoundException;

@RestControllerAdvice(assignableTypes = AirQualityController.class)
public class AirQualityControllerAdvice {

    private static final Logger LOGGER = LogManager.getLogger(AirQualityControllerAdvice.class);

    private static final String ERROR_STATION_NOT_FOUND_MSG_KEY =
            "error.airquality.stationNotFound";
    private static final String ERROR_DATA_NOT_FOUND_MSG_KEY = "error.airquality.dataNotFound";
    private static final String ERROR_INVALID_DATE_RANGE_MSG_KEY =
            "error.airquality.invalidDateRange";
    private static final String ERROR_GIOS_API_UNAVAILABLE_MSG_KEY =
            "error.airquality.giosApiUnavailable";

    private final MessageSource messageSource;

    public AirQualityControllerAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(StationNotFoundException.class)
    public ProblemDetail handleStationNotFound(StationNotFoundException ex) {
        LOGGER.warn("Station not found: {}", ex.getMessage());
        String message =
                messageSource.getMessage(
                        ERROR_STATION_NOT_FOUND_MSG_KEY, null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler(AirQualityDataNotFoundException.class)
    public ProblemDetail handleAirQualityDataNotFound(AirQualityDataNotFoundException ex) {
        LOGGER.warn("Air quality data not found: {}", ex.getMessage());
        String message =
                messageSource.getMessage(
                        ERROR_DATA_NOT_FOUND_MSG_KEY, null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ProblemDetail handleInvalidDateRange(InvalidDateRangeException ex) {
        LOGGER.warn("Invalid date range: {}", ex.getMessage());
        String message =
                messageSource.getMessage(
                        ERROR_INVALID_DATE_RANGE_MSG_KEY, null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(GiosApiException.class)
    public ProblemDetail handleGiosApiException(GiosApiException ex) {
        LOGGER.error("GIOS API error: {}", ex.getMessage(), ex);
        String message =
                messageSource.getMessage(
                        ERROR_GIOS_API_UNAVAILABLE_MSG_KEY, null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
