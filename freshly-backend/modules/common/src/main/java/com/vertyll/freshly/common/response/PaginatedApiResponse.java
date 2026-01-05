package com.vertyll.freshly.common.response;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PaginatedApiResponse<T> extends BaseResponse<PageResponse<T>> {

    public static <T> ResponseEntity<PaginatedApiResponse<T>> buildResponse(
            Page<T> page, String message, HttpStatus status) {

        PageResponse<T> pageResponse = PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();

        PaginatedApiResponse<T> response = PaginatedApiResponse.<T>builder()
                .data(pageResponse)
                .message(message)
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        return new ResponseEntity<>(response, status);
    }
}
