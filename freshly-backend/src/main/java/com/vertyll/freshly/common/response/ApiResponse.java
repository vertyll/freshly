package com.vertyll.freshly.common.response;

import java.time.LocalDateTime;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ApiResponse<T> extends BaseResponse<T> {

    public static <T> ResponseEntity<ApiResponse<T>> buildResponse(
            T data, String message, HttpStatus status) {
        ApiResponse<T> response =
                ApiResponse.<T>builder()
                        .data(data)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build();

        return new ResponseEntity<>(response, status);
    }
}
