package com.vertyll.freshly.common.exception;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LogManager.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_FAILED = "Validation failed for one or more fields";
    private static final String INVALID_VALUE = "Invalid value";
    private static final String RESOURCE_NOT_FOUND = "The requested resource was not found";
    private static final String UNEXPECTED_ERROR =
            "An unexpected error occurred. Please contact support.";
    private static final String UNKNOWN_TYPE = "unknown";

    private static final String ERRORS_PROPERTY = "errors";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        LOGGER.warn("Validation error: {}", ex.getMessage());

        Map<String, List<String>> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .collect(
                                Collectors.groupingBy(
                                        FieldError::getField,
                                        Collectors.mapping(
                                                error ->
                                                        error.getDefaultMessage() != null
                                                                ? error.getDefaultMessage()
                                                                : INVALID_VALUE,
                                                Collectors.toList())));

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, VALIDATION_FAILED);
        problemDetail.setProperty(ERRORS_PROPERTY, errors);

        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        LOGGER.warn("Type mismatch error: {}", ex.getMessage());

        Class<?> requiredType = ex.getRequiredType();
        String message =
                String.format(
                        "Parameter '%s' should be of type %s",
                        ex.getName(),
                        requiredType != null ? requiredType.getSimpleName() : UNKNOWN_TYPE);

        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        LOGGER.warn("Resource not found: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        LOGGER.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
    }
}
