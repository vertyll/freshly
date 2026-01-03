package com.vertyll.freshly.common.response;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ApiResponse<T> extends BaseResponse<T> {

    /**
     * Builds response with raw message (not localized).
     * Use this for backward compatibility or when message doesn't need i18n.
     * 
     * @deprecated Use buildResponse(T, String, MessageSource, HttpStatus) for i18n support
     */
    @Deprecated(since = "1.1")
    public static <T> ResponseEntity<ApiResponse<T>> buildResponse(
            @Nullable T data,
            String message,
            HttpStatus status
    ) {
        ApiResponse<T> response =
                ApiResponse.<T>builder()
                        .data(data)
                        .message(message)
                        .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                        .build();

        return new ResponseEntity<>(response, status);
    }

    /**
     * Builds response with localized message using MessageSource.
     * This is the recommended method for all new code.
     * 
     * @param data Response payload
     * @param messageKey Message key from messages_*.properties
     * @param messageSource Spring MessageSource bean
     * @param status HTTP status
     * @return ResponseEntity with localized message
     */
    public static <T> ResponseEntity<ApiResponse<T>> buildResponse(
            @Nullable T data,
            String messageKey,
            MessageSource messageSource,
            HttpStatus status
    ) {
        String localizedMessage = messageSource.getMessage(
                messageKey, 
                null, 
                LocaleContextHolder.getLocale()
        );
        
        ApiResponse<T> response =
                ApiResponse.<T>builder()
                        .data(data)
                        .message(localizedMessage)
                        .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                        .build();

        return new ResponseEntity<>(response, status);
    }
}
