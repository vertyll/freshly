package com.vertyll.freshly.airquality.api;

import com.vertyll.freshly.airquality.domain.exception.AirQualityDataNotFoundException;
import com.vertyll.freshly.airquality.domain.exception.GiosApiException;
import com.vertyll.freshly.airquality.domain.exception.InvalidDateRangeException;
import com.vertyll.freshly.airquality.domain.exception.StationNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AirQualityController.class)
public class AirQualityControllerAdvice {

    private final MessageSource messageSource;

    private static final Logger logger = LogManager.getLogger(AirQualityControllerAdvice.class);

    public AirQualityControllerAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(StationNotFoundException.class)
    public ProblemDetail handleStationNotFound(StationNotFoundException ex) {
        logger.warn("Station not found: {}", ex.getMessage());
        String message = messageSource.getMessage("error.airquality.stationNotFound", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler(AirQualityDataNotFoundException.class)
    public ProblemDetail handleAirQualityDataNotFound(AirQualityDataNotFoundException ex) {
        logger.warn("Air quality data not found: {}", ex.getMessage());
        String message = messageSource.getMessage("error.airquality.dataNotFound", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ProblemDetail handleInvalidDateRange(InvalidDateRangeException ex) {
        logger.warn("Invalid date range: {}", ex.getMessage());
        String message = messageSource.getMessage("error.airquality.invalidDateRange", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(GiosApiException.class)
    public ProblemDetail handleGiosApiException(GiosApiException ex) {
        logger.error("GIOS API error: {}", ex.getMessage(), ex);
        String message = messageSource.getMessage("error.airquality.giosApiUnavailable", null, LocaleContextHolder.getLocale());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
