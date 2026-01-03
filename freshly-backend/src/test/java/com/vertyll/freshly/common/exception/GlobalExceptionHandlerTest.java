package com.vertyll.freshly.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with field errors")
    void shouldHandleMethodArgumentNotValidException() throws NoSuchMethodException {
        // Given
        FieldError fieldError1 = new FieldError("userDto", "username", "Username is required");
        FieldError fieldError2 = new FieldError("userDto", "email", "Email is invalid");
        FieldError fieldError3 = new FieldError("userDto", "username", "Username must be unique");
        
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2, fieldError3));
        
        MethodParameter methodParameter = new MethodParameter(
                this.getClass().getMethod("dummyMethod"),
                -1
        );
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When
        ProblemDetail problemDetail = exceptionHandler.handleValidationExceptions(exception);

        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Validation failed for one or more fields");
        
        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors = (Map<String, List<String>>) problemDetail.getProperties().get("errors");
        assertThat(errors).isNotNull();
        assertThat(errors.get("username")).containsExactlyInAnyOrder("Username is required", "Username must be unique");
        assertThat(errors.get("email")).containsExactly("Email is invalid");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with null message")
    void shouldHandleMethodArgumentNotValidExceptionWithNullMessage() throws NoSuchMethodException {
        // Given
        FieldError fieldError = new FieldError("userDto", "age", null);
        
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        
        MethodParameter methodParameter = new MethodParameter(
                this.getClass().getMethod("dummyMethod"),
                -1
        );
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When
        ProblemDetail problemDetail = exceptionHandler.handleValidationExceptions(exception);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors = (Map<String, List<String>>) problemDetail.getProperties().get("errors");
        assertThat(errors).isNotNull();
        assertThat(errors.get("age")).containsExactly("Invalid value");
    }

    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException")
    void shouldHandleMethodArgumentTypeMismatchException() {
        // Given
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
            "abc",
            Integer.class,
            "id",
            null,
            null
        );

        // When
        ProblemDetail problemDetail = exceptionHandler.handleTypeMismatch(exception);

        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getDetail()).contains("Parameter 'id' should be of type Integer");
    }

    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException with null type")
    void shouldHandleMethodArgumentTypeMismatchExceptionWithNullType() {
        // Given
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
            "abc",
            null,
            "id",
            null,
            null
        );

        // When
        ProblemDetail problemDetail = exceptionHandler.handleTypeMismatch(exception);

        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getDetail()).contains("Parameter 'id' should be of type unknown");
    }

    @Test
    @DisplayName("Should handle NoResourceFoundException")
    void shouldHandleNoResourceFoundException() {
        // Given
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/api/test", "Resource not found");

        // When
        ProblemDetail problemDetail = exceptionHandler.handleNoResourceFound(exception);

        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getDetail()).isEqualTo("The requested resource was not found");
    }

    @Test
    @DisplayName("Should handle AuthorizationDeniedException")
    void shouldHandleAuthorizationDeniedException() {
        // Given
        AuthorizationDeniedException exception = new AuthorizationDeniedException(
                "Access denied",
                new AuthorizationDecision(false)
        );

        // When
        ProblemDetail problemDetail = exceptionHandler.handleAuthorizationDenied(exception);

        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problemDetail.getDetail()).isEqualTo("You do not have permission to access this resource");
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void shouldHandleGenericException() {
        // Given
        Exception exception = new RuntimeException("Something went wrong");

        // When
        ProblemDetail problemDetail = exceptionHandler.handleGenericException(exception);

        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getDetail()).isEqualTo("An unexpected error occurred. Please contact support.");
    }

    @Test
    @DisplayName("Should handle NullPointerException as generic exception")
    void shouldHandleNullPointerException() {
        // Given
        NullPointerException exception = new NullPointerException("Null pointer");

        // When
        ProblemDetail problemDetail = exceptionHandler.handleGenericException(exception);

        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getDetail()).isEqualTo("An unexpected error occurred. Please contact support.");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException as generic exception")
    void shouldHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        // When
        ProblemDetail problemDetail = exceptionHandler.handleGenericException(exception);

        // Then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getDetail()).isEqualTo("An unexpected error occurred. Please contact support.");
    }

    // Helper method for MethodParameter mock
    public void dummyMethod() {
        // Used only for getting Method reference in tests
    }
}
