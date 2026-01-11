package com.vertyll.freshly.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PaginatedApiResponseTest {

    @Test
    @DisplayName("Should build paginated response with data")
    void shouldBuildPaginatedResponseWithData() {
        // Given
        List<String> content = Arrays.asList("item1", "item2", "item3");
        Page<String> page = new PageImpl<>(content, PageRequest.of(0, 10), 3);
        String message = "Success";

        // When
        ResponseEntity<PaginatedApiResponse<String>> response =
                PaginatedApiResponse.buildResponse(page, message, HttpStatus.OK);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PaginatedApiResponse<String> body = Objects.requireNonNull(response.getBody());
        assertThat(body.getMessage()).isEqualTo(message);
        assertThat(body.getTimestamp()).isNotNull();

        PageResponse<String> pageResponse = Objects.requireNonNull(body.getData());
        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.getContent()).hasSize(3);
        assertThat(pageResponse.getContent()).containsExactly("item1", "item2", "item3");
        assertThat(pageResponse.getPageNumber()).isZero();
        assertThat(pageResponse.getPageSize()).isEqualTo(10);
        assertThat(pageResponse.getTotalElements()).isEqualTo(3);
        assertThat(pageResponse.getTotalPages()).isEqualTo(1);
        assertThat(pageResponse.isFirst()).isTrue();
        assertThat(pageResponse.isLast()).isTrue();
        assertThat(pageResponse.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should build paginated response for second page")
    void shouldBuildPaginatedResponseForSecondPage() {
        // Given
        List<String> content = Arrays.asList("item6", "item7", "item8", "item9", "item10");
        Page<String> page = new PageImpl<>(content, PageRequest.of(1, 5), 12);
        String message = "Page 2 of results";

        // When
        ResponseEntity<PaginatedApiResponse<String>> response =
                PaginatedApiResponse.buildResponse(page, message, HttpStatus.OK);

        // Then
        PaginatedApiResponse<String> body = Objects.requireNonNull(response.getBody());

        PageResponse<String> pageResponse = Objects.requireNonNull(body.getData());
        assertThat(pageResponse.getPageNumber()).isEqualTo(1);
        assertThat(pageResponse.getPageSize()).isEqualTo(5);
        assertThat(pageResponse.getTotalElements()).isEqualTo(12);
        assertThat(pageResponse.getTotalPages()).isEqualTo(3);
        assertThat(pageResponse.isFirst()).isFalse();
        assertThat(pageResponse.isLast()).isFalse();
        assertThat(pageResponse.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should build paginated response for empty page")
    void shouldBuildPaginatedResponseForEmptyPage() {
        // Given
        Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        String message = "No results found";

        // When
        ResponseEntity<PaginatedApiResponse<String>> response =
                PaginatedApiResponse.buildResponse(page, message, HttpStatus.OK);

        // Then
        PaginatedApiResponse<String> body = Objects.requireNonNull(response.getBody());

        PageResponse<String> pageResponse = Objects.requireNonNull(body.getData());
        assertThat(pageResponse.getContent()).isEmpty();
        assertThat(pageResponse.getPageNumber()).isZero();
        assertThat(pageResponse.getTotalElements()).isZero();
        assertThat(pageResponse.getTotalPages()).isZero();
        assertThat(pageResponse.isFirst()).isTrue();
        assertThat(pageResponse.isLast()).isTrue();
        assertThat(pageResponse.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Should build paginated response for last page")
    void shouldBuildPaginatedResponseForLastPage() {
        // Given
        List<String> content = Arrays.asList("item21", "item22");
        Page<String> page = new PageImpl<>(content, PageRequest.of(2, 10), 22);
        String message = "Last page";

        // When
        ResponseEntity<PaginatedApiResponse<String>> response =
                PaginatedApiResponse.buildResponse(page, message, HttpStatus.OK);

        // Then
        PaginatedApiResponse<String> body = Objects.requireNonNull(response.getBody());

        PageResponse<String> pageResponse = Objects.requireNonNull(body.getData());
        assertThat(pageResponse.getContent()).hasSize(2);
        assertThat(pageResponse.getPageNumber()).isEqualTo(2);
        assertThat(pageResponse.getTotalPages()).isEqualTo(3);
        assertThat(pageResponse.isFirst()).isFalse();
        assertThat(pageResponse.isLast()).isTrue();
        assertThat(pageResponse.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Should build paginated response with different status codes")
    void shouldBuildPaginatedResponseWithDifferentStatusCodes() {
        // Given
        List<Integer> content = Arrays.asList(1, 2, 3);
        Page<Integer> page = new PageImpl<>(content, PageRequest.of(0, 10), 3);
        String message = "Created successfully";

        // When
        ResponseEntity<PaginatedApiResponse<Integer>> response =
                PaginatedApiResponse.buildResponse(page, message, HttpStatus.CREATED);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PaginatedApiResponse<Integer> body = Objects.requireNonNull(response.getBody());
        assertThat(body.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should include timestamp in paginated response")
    void shouldIncludeTimestampInPaginatedResponse() {
        // Given
        List<String> content = List.of("item1");
        Page<String> page = new PageImpl<>(content, PageRequest.of(0, 10), 1);
        String message = "Success";

        // When
        ResponseEntity<PaginatedApiResponse<String>> response =
                PaginatedApiResponse.buildResponse(page, message, HttpStatus.OK);

        // Then
        PaginatedApiResponse<String> body = Objects.requireNonNull(response.getBody());
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getTimestamp())
                .isBeforeOrEqualTo(java.time.LocalDateTime.now(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Should handle single element page")
    void shouldHandleSingleElementPage() {
        // Given
        List<String> content = List.of("only-item");
        Page<String> page = new PageImpl<>(content, PageRequest.of(0, 1), 1);
        String message = "Single item";

        // When
        ResponseEntity<PaginatedApiResponse<String>> response =
                PaginatedApiResponse.buildResponse(page, message, HttpStatus.OK);

        // Then
        PaginatedApiResponse<String> body = Objects.requireNonNull(response.getBody());

        PageResponse<String> pageResponse = Objects.requireNonNull(body.getData());
        assertThat(pageResponse.getContent()).hasSize(1);
        assertThat(pageResponse.getPageSize()).isEqualTo(1);
        assertThat(pageResponse.getTotalElements()).isEqualTo(1);
        assertThat(pageResponse.getTotalPages()).isEqualTo(1);
        assertThat(pageResponse.isFirst()).isTrue();
        assertThat(pageResponse.isLast()).isTrue();
    }
}
