package com.vertyll.freshly.security.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;

@ExtendWith(MockitoExtension.class)
class SecurityExceptionHandlerTest {
    private static final String ACCESS_DENIED = "Access denied";
    private static final String INSUFFICIENT_PRIVILEGES = "Insufficient privileges";
    private static final String PERMISSION_DENIED_DETAIL =
            "You do not have permission to access this resource";

    private SecurityExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new SecurityExceptionHandler();
    }

    @Test
    @DisplayName("Should handle AuthorizationDeniedException")
    void shouldHandleAuthorizationDeniedException() {
        // Given
        AuthorizationDeniedException exception =
                new AuthorizationDeniedException(ACCESS_DENIED, new AuthorizationDecision(false));

        // When
        ProblemDetail problemDetail = exceptionHandler.handleAuthorizationDenied(exception);

        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problemDetail.getDetail()).isEqualTo(PERMISSION_DENIED_DETAIL);
    }

    @Test
    @DisplayName("Should handle AuthorizationDeniedException with custom message")
    void shouldHandleAuthorizationDeniedExceptionWithCustomMessage() {
        // Given
        AuthorizationDeniedException exception =
                new AuthorizationDeniedException(
                        INSUFFICIENT_PRIVILEGES, new AuthorizationDecision(false));

        // When
        ProblemDetail problemDetail = exceptionHandler.handleAuthorizationDenied(exception);

        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problemDetail.getDetail()).isEqualTo(PERMISSION_DENIED_DETAIL);
    }
}
