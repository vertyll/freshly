package com.vertyll.freshly.web.error;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import com.vertyll.freshly.lang.error.ErrorKind;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHttpStatusMapperTest {
    @ParameterizedTest
    @CsvSource(
        {
            "NOT_FOUND, NOT_FOUND",
            "UNAUTHENTICATED, UNAUTHORIZED",
            "ACCESS_DENIED, FORBIDDEN",
            "CONFLICT, CONFLICT",
            "INVALID, BAD_REQUEST",
            "PRECONDITION_FAILED, PRECONDITION_FAILED",
            "GONE, GONE",
            "MISCONFIGURED, INTERNAL_SERVER_ERROR",
            "EXTERNAL_SERVICE_FAILURE, BAD_GATEWAY"
        }
    )
    void eachKindOfFailureHasItsStatus(ErrorKind kind, HttpStatus status) {
        assertThat(ErrorHttpStatusMapper.statusOf(kind)).isEqualTo(status);
    }
}
