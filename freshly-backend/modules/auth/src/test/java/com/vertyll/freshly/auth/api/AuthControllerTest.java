package com.vertyll.freshly.auth.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.vertyll.freshly.auth.api.dto.*;
import com.vertyll.freshly.auth.application.AuthService;
import com.vertyll.freshly.auth.domain.exception.InvalidPasswordException;
import com.vertyll.freshly.common.config.CookieProperties;
import com.vertyll.freshly.common.config.JwtProperties;
import com.vertyll.freshly.common.exception.GlobalExceptionHandler;
import com.vertyll.freshly.security.resolver.RefreshTokenCookieArgumentResolver;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String REFRESH_TOKEN_KEY = "refresh_token";
    private static final String COOKIE_PATH_KEY = "/";
    private static final String SAME_SITE_LAX_KEY = "Lax";
    private static final String BEARER_KEY = "Bearer";

    private static final String SUCCESS_REGISTERED_MSG_KEY = "success.auth.registered";
    private static final String SUCCESS_EMAIL_VERIFIED_MSG_KEY = "success.auth.emailVerified";
    private static final String SUCCESS_LOGIN_MSG_KEY = "success.auth.loginSuccessful";
    private static final String SUCCESS_TOKEN_REFRESHED_MSG_KEY = "success.auth.tokenRefreshed";
    private static final String SUCCESS_LOGOUT_MSG_KEY = "success.auth.logoutSuccessful";
    private static final String SUCCESS_RESET_EMAIL_SENT_MSG_KEY = "success.auth.resetEmailSent";
    private static final String SUCCESS_PASSWORD_RESET_MSG_KEY = "success.auth.passwordReset";
    private static final String SUCCESS_PASSWORD_CHANGED_MSG_KEY = "success.auth.passwordChanged";
    private static final String SUCCESS_EMAIL_CHANGE_INITIATED_MSG_KEY =
            "success.auth.emailChangeInitiated";
    private static final String ERROR_INVALID_PASSWORD_MSG_KEY = "error.auth.invalidPassword";

    private static final String REGISTERED = "User registered successfully";
    private static final String EMAIL_VERIFIED = "Email verified successfully";
    private static final String LOGIN_SUCCESS = "Login successful";
    private static final String TOKEN_REFRESHED = "Token refreshed successfully";
    private static final String LOGOUT_SUCCESS = "Logout successful";
    private static final String RESET_EMAIL_SENT = "Password reset email sent";
    private static final String PASSWORD_RESET = "Password reset successfully";
    private static final String PASSWORD_CHANGED = "Password changed successfully";
    private static final String EMAIL_CHANGE_INITIATED = "Email change initiated";
    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private MockMvc mockMvc;
    @Mock private AuthService authService;
    @Mock private CookieProperties cookieProperties;
    @Mock private JwtProperties jwtProperties;
    @Mock private MessageSource messageSource;
    @Mock private JwtProperties.RefreshToken refreshTokenProperties;
    @InjectMocks private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        lenient().when(jwtProperties.refreshToken()).thenReturn(refreshTokenProperties);
        lenient().when(refreshTokenProperties.cookieName()).thenReturn(REFRESH_TOKEN_KEY);

        RefreshTokenCookieArgumentResolver refreshTokenResolver =
                new RefreshTokenCookieArgumentResolver(jwtProperties);

        AuthenticationPrincipalArgumentResolver authenticationPrincipalResolver =
                new AuthenticationPrincipalArgumentResolver();

        mockMvc =
                MockMvcBuilders.standaloneSetup(authController)
                        .setControllerAdvice(
                                new AuthControllerAdvice(messageSource),
                                new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(
                                refreshTokenResolver, authenticationPrincipalResolver)
                        .setValidator(validator)
                        .build();
    }

    @AfterEach
    void tearDown() {
        if (validator != null) {
            validator.close();
        }
    }

    @Nested
    @DisplayName("User Registration Tests")
    class UserRegistrationTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() throws Exception {
            RegisterUserRequestDto request =
                    new RegisterUserRequestDto(
                            "testuser", "test@example.com", "Password123!", "John", "Doe");

            UUID userId = UUID.randomUUID();

            when(authService.registerUser(any(RegisterUserRequestDto.class))).thenReturn(userId);

            when(messageSource.getMessage(eq(SUCCESS_REGISTERED_MSG_KEY), any(), any()))
                    .thenReturn(REGISTERED);

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.message").value(REGISTERED));

            verify(authService).registerUser(any(RegisterUserRequestDto.class));
        }

        @Test
        @DisplayName("Should fail with invalid request data")
        void shouldFailWithInvalidRequestData() throws Exception {
            RegisterUserRequestDto request =
                    new RegisterUserRequestDto(
                            "testuser", "invalid-email", "Password123!", "John", "Doe");

            mockMvc.perform(
                            post("/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any());
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email successfully")
        void shouldVerifyEmailSuccessfully() throws Exception {
            String token = "verification-token";

            when(messageSource.getMessage(eq(SUCCESS_EMAIL_VERIFIED_MSG_KEY), any(), any()))
                    .thenReturn(EMAIL_VERIFIED);

            doNothing().when(authService).verifyEmail(token);

            mockMvc.perform(get("/auth/verify-email").param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(EMAIL_VERIFIED));

            verify(authService).verifyEmail(token);
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully")
        void shouldLoginUserSuccessfully() throws Exception {
            LoginRequestDto request = new LoginRequestDto("testuser", "password123");
            TokenResponseDto tokenResponse =
                    new TokenResponseDto("access-token", "refresh-token", 3600, 86400, BEARER_KEY);

            when(authService.login(any(LoginRequestDto.class))).thenReturn(tokenResponse);

            when(refreshTokenProperties.expiration()).thenReturn(86400000L);
            when(cookieProperties.httpOnly()).thenReturn(true);
            when(cookieProperties.secure()).thenReturn(false);
            when(cookieProperties.path()).thenReturn(COOKIE_PATH_KEY);
            when(cookieProperties.sameSite()).thenReturn(SAME_SITE_LAX_KEY);

            when(messageSource.getMessage(eq(SUCCESS_LOGIN_MSG_KEY), any(), any()))
                    .thenReturn(LOGIN_SUCCESS);

            mockMvc.perform(
                            post("/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").value("access-token"))
                    .andExpect(jsonPath("$.data.token_type").value(BEARER_KEY))
                    .andExpect(jsonPath("$.message").value(LOGIN_SUCCESS));

            verify(authService).login(any(LoginRequestDto.class));
        }

        @Test
        @DisplayName("Should fail with invalid credentials")
        void shouldFailWithInvalidCredentials() throws Exception {
            LoginRequestDto request = new LoginRequestDto("testuser", "wrongpassword");

            when(authService.login(any(LoginRequestDto.class)))
                    .thenThrow(new InvalidPasswordException(INVALID_CREDENTIALS));

            when(messageSource.getMessage(eq(ERROR_INVALID_PASSWORD_MSG_KEY), any(), any()))
                    .thenReturn(INVALID_CREDENTIALS);

            mockMvc.perform(
                            post("/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(authService).login(any(LoginRequestDto.class));
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    class TokenRefreshTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            String refreshToken = "refresh-token";
            TokenResponseDto tokenResponse =
                    new TokenResponseDto(
                            "new-access-token", "new-refresh-token", 3600, 86400, BEARER_KEY);

            when(authService.refreshToken(refreshToken)).thenReturn(tokenResponse);

            when(refreshTokenProperties.expiration()).thenReturn(86400000L);
            when(cookieProperties.httpOnly()).thenReturn(true);
            when(cookieProperties.secure()).thenReturn(false);
            when(cookieProperties.path()).thenReturn(COOKIE_PATH_KEY);
            when(cookieProperties.sameSite()).thenReturn(SAME_SITE_LAX_KEY);

            when(messageSource.getMessage(eq(SUCCESS_TOKEN_REFRESHED_MSG_KEY), any(), any()))
                    .thenReturn(TOKEN_REFRESHED);

            mockMvc.perform(
                            post("/auth/refresh")
                                    .cookie(new Cookie(REFRESH_TOKEN_KEY, refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(TOKEN_REFRESHED));

            verify(authService).refreshToken(refreshToken);
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout user successfully")
        void shouldLogoutUserSuccessfully() throws Exception {
            String refreshToken = "refresh-token";

            when(cookieProperties.httpOnly()).thenReturn(true);
            when(cookieProperties.secure()).thenReturn(false);
            when(cookieProperties.path()).thenReturn(COOKIE_PATH_KEY);
            when(cookieProperties.sameSite()).thenReturn(SAME_SITE_LAX_KEY);

            when(messageSource.getMessage(eq(SUCCESS_LOGOUT_MSG_KEY), any(), any()))
                    .thenReturn(LOGOUT_SUCCESS);

            doNothing().when(authService).logout(refreshToken);

            mockMvc.perform(
                            post("/auth/logout")
                                    .cookie(new Cookie(REFRESH_TOKEN_KEY, refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(LOGOUT_SUCCESS));

            verify(authService).logout(refreshToken);
        }
    }

    @Nested
    @DisplayName("Forgot Password Tests")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should initiate password reset successfully")
        void shouldInitiatePasswordResetSuccessfully() throws Exception {
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("test@example.com");

            when(messageSource.getMessage(eq(SUCCESS_RESET_EMAIL_SENT_MSG_KEY), any(), any()))
                    .thenReturn(RESET_EMAIL_SENT);

            mockMvc.perform(
                            post("/auth/forgot-password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(RESET_EMAIL_SENT));
        }
    }

    @Nested
    @DisplayName("Reset Password Tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password successfully")
        void shouldResetPasswordSuccessfully() throws Exception {
            ResetPasswordRequestDto request = new ResetPasswordRequestDto("token", "NewPass123!");

            when(messageSource.getMessage(eq(SUCCESS_PASSWORD_RESET_MSG_KEY), any(), any()))
                    .thenReturn(PASSWORD_RESET);

            mockMvc.perform(
                            post("/auth/reset-password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(PASSWORD_RESET));
        }
    }

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() throws Exception {
            // Given
            ChangePasswordRequestDto request =
                    new ChangePasswordRequestDto("currentPassword123", "NewPassword123!");

            UUID userId = UUID.randomUUID();

            setupSecurityContext(userId);

            // DODAJ TO:
            doNothing()
                    .when(authService)
                    .changePassword(eq(userId), any(ChangePasswordRequestDto.class));

            when(messageSource.getMessage(eq(SUCCESS_PASSWORD_CHANGED_MSG_KEY), any(), any()))
                    .thenReturn(PASSWORD_CHANGED);

            mockMvc.perform(
                            put("/auth/change-password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(PASSWORD_CHANGED));

            verify(authService).changePassword(eq(userId), any(ChangePasswordRequestDto.class));
        }
    }

    @Nested
    @DisplayName("Change Email Tests")
    class ChangeEmailTests {

        @Test
        @DisplayName("Should change email successfully")
        void shouldChangeEmailSuccessfully() throws Exception {
            ChangeEmailRequestDto request = new ChangeEmailRequestDto("new@test.com");
            UUID userId = UUID.randomUUID();

            setupSecurityContext(userId);

            when(messageSource.getMessage(eq(SUCCESS_EMAIL_CHANGE_INITIATED_MSG_KEY), any(), any()))
                    .thenReturn(EMAIL_CHANGE_INITIATED);

            mockMvc.perform(
                            put("/auth/change-email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(EMAIL_CHANGE_INITIATED));
        }
    }

    private void setupSecurityContext(UUID userId) {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject(userId.toString())
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build();

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt, Collections.emptyList());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
