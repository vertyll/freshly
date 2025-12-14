package com.vertyll.freshly.common.response;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ApiResponse<T> extends BaseResponse<T> {

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
}
