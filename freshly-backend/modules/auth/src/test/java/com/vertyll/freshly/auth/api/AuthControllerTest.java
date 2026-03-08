package com.vertyll.freshly.auth.api;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import com.vertyll.freshly.auth.api.dto.AccessTokenResponseDto;
import com.vertyll.freshly.auth.api.dto.AuthResponseDto;
import com.vertyll.freshly.auth.api.dto.ChangeEmailRequestDto;
import com.vertyll.freshly.auth.api.dto.ChangePasswordRequestDto;
import com.vertyll.freshly.auth.api.dto.ForgotPasswordRequestDto;
import com.vertyll.freshly.auth.api.dto.LoginRequestDto;
import com.vertyll.freshly.auth.api.dto.RegisterUserRequestDto;
import com.vertyll.freshly.auth.api.dto.ResetPasswordRequestDto;
import com.vertyll.freshly.auth.api.dto.TokenResponseDto;
import com.vertyll.freshly.auth.application.AuthService;
import com.vertyll.freshly.common.config.KeycloakProperties;
import com.vertyll.freshly.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String SUCCESS = "Success";
    private static final String AUTH_REGISTERED_MSG_KEY = "success.auth.registered";
    private static final String AUTH_EMAIL_VERIFIED_MSG_KEY = "success.auth.emailVerified";
    private static final String AUTH_LOGIN_SUCCESSFUL_MSG_KEY = "success.auth.loginSuccessful";
    private static final String AUTH_LOGOUT_SUCCESSFUL_MSG_KEY = "success.auth.logoutSuccessful";
    private static final String AUTH_RESET_EMAIL_SENT_MSG_KEY = "success.auth.resetEmailSent";
    private static final String AUTH_PASSWORD_RESET_MSG_KEY = "success.auth.passwordReset";
    private static final String AUTH_PASSWORD_CHANGED_MSG_KEY = "success.auth.passwordChanged";
    private static final String AUTH_EMAIL_CHANGE_INITIATED_MSG_KEY =
            "success.auth.emailChangeInitiated";
    private static final String AUTH_TOKEN_REFRESHED_MSG_KEY = "success.auth.tokenRefreshed";

    @Mock
    @SuppressWarnings("NullAway.Init")
    private AuthService authService;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private MessageSource messageSource;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private KeycloakProperties keycloakProperties;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private KeycloakProperties.CookieProperties cookieProperties;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private HttpServletResponse httpServletResponse;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private Jwt jwt;

    private AuthController authController;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        authController = new AuthController(authService, messageSource, keycloakProperties);
    }

    private void setupCookieProperties() {
        when(keycloakProperties.cookie()).thenReturn(cookieProperties);
        when(cookieProperties.refreshTokenCookieName()).thenReturn("refresh_token");
        when(cookieProperties.httpOnly()).thenReturn(true);
        when(cookieProperties.secure()).thenReturn(true);
        when(cookieProperties.sameSite()).thenReturn("Strict");
        when(cookieProperties.path()).thenReturn("/auth");
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID userId = UUID.randomUUID();
        RegisterUserRequestDto request =
                new RegisterUserRequestDto(
                        "testuser", "test@example.com", "Password1!", "Jan", "Kowalski");

        when(authService.registerUser(request)).thenReturn(userId);

        // When
        ResponseEntity<ApiResponse<AuthResponseDto>> response = authController.register(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(authService).registerUser(request);
        verify(messageSource).getMessage(eq(AUTH_REGISTERED_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should use message source for register response")
    void shouldUseMessageSourceForRegisterResponse() {
        // Given
        UUID userId = UUID.randomUUID();
        RegisterUserRequestDto request =
                new RegisterUserRequestDto(
                        "testuser", "test@example.com", "Password1!", "Jan", "Kowalski");

        when(authService.registerUser(request)).thenReturn(userId);

        // When
        authController.register(request);

        // Then
        verify(messageSource).getMessage(eq(AUTH_REGISTERED_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should verify email with valid token")
    void shouldVerifyEmailWithValidToken() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        String token = "valid-token";
        doNothing().when(authService).verifyEmail(token);

        // When
        ResponseEntity<ApiResponse<Void>> response = authController.verifyEmail(token);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).verifyEmail(token);
        verify(messageSource).getMessage(eq(AUTH_EMAIL_VERIFIED_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should login user successfully and set refresh token cookie")
    void shouldLoginUserSuccessfullyAndSetRefreshTokenCookie() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        setupCookieProperties();

        LoginRequestDto request = new LoginRequestDto("testuser", "Password1!");
        TokenResponseDto tokens =
                new TokenResponseDto("access-token", "refresh-token", 300, 3600, "Bearer");

        when(authService.login(request)).thenReturn(tokens);

        // When
        ResponseEntity<ApiResponse<AccessTokenResponseDto>> response =
                authController.login(request, httpServletResponse);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).login(request);
        verify(httpServletResponse).addHeader(eq("Set-Cookie"), anyString());
        verify(messageSource).getMessage(eq(AUTH_LOGIN_SUCCESSFUL_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should return access token in login response body")
    void shouldReturnAccessTokenInLoginResponseBody() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        setupCookieProperties();

        LoginRequestDto request = new LoginRequestDto("testuser", "Password1!");
        TokenResponseDto tokens =
                new TokenResponseDto("my-access-token", "refresh-token", 300, 3600, "Bearer");

        when(authService.login(request)).thenReturn(tokens);

        // When
        ResponseEntity<ApiResponse<AccessTokenResponseDto>> response =
                authController.login(request, httpServletResponse);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().accessToken()).isEqualTo("my-access-token");
    }

    @Test
    @DisplayName("Should refresh access token and set new refresh token cookie")
    void shouldRefreshAccessTokenAndSetNewRefreshTokenCookie() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        setupCookieProperties();

        String refreshToken = "old-refresh-token";
        TokenResponseDto tokens =
                new TokenResponseDto("new-access-token", "new-refresh-token", 300, 3600, "Bearer");

        when(authService.refreshToken(refreshToken)).thenReturn(tokens);

        // When
        ResponseEntity<ApiResponse<AccessTokenResponseDto>> response =
                authController.refresh(refreshToken, httpServletResponse);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).refreshToken(refreshToken);
        verify(httpServletResponse).addHeader(eq("Set-Cookie"), anyString());
        verify(messageSource).getMessage(eq(AUTH_TOKEN_REFRESHED_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should refresh token when refresh token cookie is null")
    void shouldRefreshTokenWhenRefreshTokenCookieIsNull() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        setupCookieProperties();

        TokenResponseDto tokens =
                new TokenResponseDto("new-access-token", "new-refresh-token", 300, 3600, "Bearer");
        when(authService.refreshToken(null)).thenReturn(tokens);

        // When
        ResponseEntity<ApiResponse<AccessTokenResponseDto>> response =
                authController.refresh(null, httpServletResponse);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).refreshToken(null);
    }

    @Test
    @DisplayName("Should logout user and clear refresh token cookie")
    void shouldLogoutUserAndClearRefreshTokenCookie() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        setupCookieProperties();

        String refreshToken = "some-refresh-token";
        doNothing().when(authService).logout(refreshToken);

        // When
        ResponseEntity<ApiResponse<Void>> response =
                authController.logout(refreshToken, httpServletResponse);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).logout(refreshToken);
        verify(httpServletResponse).addHeader(eq("Set-Cookie"), anyString());
        verify(messageSource).getMessage(eq(AUTH_LOGOUT_SUCCESSFUL_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should initiate password reset and send email")
    void shouldInitiatePasswordResetAndSendEmail() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("test@example.com");
        doNothing().when(authService).initiatePasswordReset(request);

        // When
        ResponseEntity<ApiResponse<Void>> response = authController.forgotPassword(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).initiatePasswordReset(request);
        verify(messageSource).getMessage(eq(AUTH_RESET_EMAIL_SENT_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should reset password with valid token")
    void shouldResetPasswordWithValidToken() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        ResetPasswordRequestDto request =
                new ResetPasswordRequestDto("reset-token", "NewPassword1!");
        doNothing().when(authService).resetPassword(request);

        // When
        ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).resetPassword(request);
        verify(messageSource).getMessage(eq(AUTH_PASSWORD_RESET_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should change password for authenticated user")
    void shouldChangePasswordForAuthenticatedUser() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID userId = UUID.randomUUID();
        ChangePasswordRequestDto request =
                new ChangePasswordRequestDto("OldPassword1!", "NewPassword1!");

        when(jwt.getSubject()).thenReturn(userId.toString());
        doNothing().when(authService).changePassword(userId, request);

        // When
        ResponseEntity<ApiResponse<Void>> response = authController.changePassword(request, jwt);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(jwt).getSubject();
        verify(authService).changePassword(userId, request);
        verify(messageSource).getMessage(eq(AUTH_PASSWORD_CHANGED_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should extract user id from JWT when changing password")
    void shouldExtractUserIdFromJwtWhenChangingPassword() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID userId = UUID.randomUUID();
        ChangePasswordRequestDto request =
                new ChangePasswordRequestDto("OldPassword1!", "NewPassword1!");

        when(jwt.getSubject()).thenReturn(userId.toString());
        doNothing().when(authService).changePassword(eq(userId), any());

        // When
        authController.changePassword(request, jwt);

        // Then
        verify(jwt).getSubject();
        verify(authService).changePassword(eq(userId), any());
    }

    @Test
    @DisplayName("Should initiate email change for authenticated user")
    void shouldInitiateEmailChangeForAuthenticatedUser() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID userId = UUID.randomUUID();
        ChangeEmailRequestDto request = new ChangeEmailRequestDto("new@example.com");

        when(jwt.getSubject()).thenReturn(userId.toString());
        doNothing().when(authService).changeEmail(userId, request);

        // When
        ResponseEntity<ApiResponse<Void>> response = authController.changeEmail(request, jwt);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(jwt).getSubject();
        verify(authService).changeEmail(userId, request);
        verify(messageSource).getMessage(eq(AUTH_EMAIL_CHANGE_INITIATED_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should extract user id from JWT when changing email")
    void shouldExtractUserIdFromJwtWhenChangingEmail() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID userId = UUID.randomUUID();
        ChangeEmailRequestDto request = new ChangeEmailRequestDto("new@example.com");

        when(jwt.getSubject()).thenReturn(userId.toString());
        doNothing().when(authService).changeEmail(eq(userId), any());

        // When
        authController.changeEmail(request, jwt);

        // Then
        verify(jwt).getSubject();
        verify(authService).changeEmail(eq(userId), any());
    }
}
