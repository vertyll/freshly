package com.vertyll.freshly.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vertyll.freshly.auth.api.dto.*;
import com.vertyll.freshly.auth.application.AuthService;
import com.vertyll.freshly.auth.domain.exception.InvalidPasswordException;
import com.vertyll.freshly.common.exception.GlobalExceptionHandler;
import com.vertyll.freshly.security.config.CookieProperties;
import com.vertyll.freshly.security.config.JwtProperties;
import com.vertyll.freshly.security.resolver.RefreshTokenCookieArgumentResolver;
import jakarta.servlet.http.Cookie;
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

import java.time.Instant;
import java.util.Collections;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private CookieProperties cookieProperties;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private MessageSource messageSource;

    @Mock
    private JwtProperties.RefreshToken refreshTokenProperties;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        // Mock for RefreshTokenCookieArgumentResolver
        lenient().when(jwtProperties.refreshToken()).thenReturn(refreshTokenProperties);
        lenient().when(refreshTokenProperties.cookieName()).thenReturn("refresh_token");

        RefreshTokenCookieArgumentResolver refreshTokenResolver = new RefreshTokenCookieArgumentResolver(jwtProperties);

        AuthenticationPrincipalArgumentResolver authenticationPrincipalResolver = new AuthenticationPrincipalArgumentResolver();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new AuthControllerAdvice(messageSource), new GlobalExceptionHandler())
                .setCustomArgumentResolvers(refreshTokenResolver, authenticationPrincipalResolver)
                .setValidator(validator)
                .build();
    }

    @Nested
    @DisplayName("User Registration Tests")
    class UserRegistrationTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() throws Exception {
            // Given
            RegisterUserRequestDto request = new RegisterUserRequestDto(
                    "testuser",
                    "test@example.com",
                    "Password123!",
                    "John",
                    "Doe"
            );

            UUID userId = UUID.randomUUID();

            when(authService.registerUser(any(RegisterUserRequestDto.class)))
                    .thenReturn(userId);

            when(messageSource.getMessage(eq("success.auth.registered"), any(), any()))
                    .thenReturn("User registered successfully");

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.message").value("User registered successfully"));

            verify(authService).registerUser(any(RegisterUserRequestDto.class));
        }

        @Test
        @DisplayName("Should fail with invalid request data")
        void shouldFailWithInvalidRequestData() throws Exception {
            // Given - invalid email
            RegisterUserRequestDto request = new RegisterUserRequestDto(
                    "testuser",
                    "invalid-email",
                    "Password123!",
                    "John",
                    "Doe"
            );

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any());
        }

        @Test
        @DisplayName("Should fail with missing required fields")
        void shouldFailWithMissingRequiredFields() throws Exception {
            // Given - missing email
            String invalidRequest = """
                    {
                        "username": "testuser",
                        "password": "Password123!",
                        "firstName": "John",
                        "lastName": "Doe"
                    }
                    """;

            // When & Then
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
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
            // Given
            String token = "verification-token";

            when(messageSource.getMessage(eq("success.auth.emailVerified"), any(), any()))
                    .thenReturn("Email verified successfully");

            doNothing().when(authService).verifyEmail(token);

            // When & Then
            mockMvc.perform(get("/auth/verify-email")
                            .param("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Email verified successfully"));

            verify(authService).verifyEmail(token);
        }

        @Test
        @DisplayName("Should fail without token parameter")
        void shouldFailWithoutTokenParameter() throws Exception {
            // When & Then
            mockMvc.perform(get("/auth/verify-email"))
                    .andExpect(status().isInternalServerError());

            verify(authService, never()).verifyEmail(any());
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully")
        void shouldLoginUserSuccessfully() throws Exception {
            // Given
            LoginRequestDto request = new LoginRequestDto("testuser", "password123");
            TokenResponseDto tokenResponse = new TokenResponseDto(
                    "access-token",
                    "refresh-token",
                    3600,
                    86400,
                    "Bearer"
            );

            when(authService.login(any(LoginRequestDto.class)))
                    .thenReturn(tokenResponse);

            when(refreshTokenProperties.expiration()).thenReturn(86400000L);
            when(cookieProperties.httpOnly()).thenReturn(true);
            when(cookieProperties.secure()).thenReturn(false);
            when(cookieProperties.path()).thenReturn("/");
            when(cookieProperties.sameSite()).thenReturn("Lax");

            when(messageSource.getMessage(eq("success.auth.loginSuccessful"), any(), any()))
                    .thenReturn("Login successful");

            // When & Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").value("access-token"))
                    .andExpect(jsonPath("$.data.expires_in").value(3600))
                    .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.message").value("Login successful"))
                    .andExpect(header().exists("Set-Cookie"))
                    .andExpect(header().string("Set-Cookie", containsString("refresh_token")));

            verify(authService).login(any(LoginRequestDto.class));
        }

        @Test
        @DisplayName("Should fail with invalid credentials")
        void shouldFailWithInvalidCredentials() throws Exception {
            // Given
            LoginRequestDto request = new LoginRequestDto("testuser", "wrongpassword");

            when(authService.login(any(LoginRequestDto.class)))
                    .thenThrow(new InvalidPasswordException("Invalid credentials"));

            when(messageSource.getMessage(eq("error.auth.invalidPassword"), any(), any()))
                    .thenReturn("Invalid credentials");

            // When & Then
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
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
            // Given
            String refreshToken = "refresh-token";
            TokenResponseDto tokenResponse = new TokenResponseDto(
                    "new-access-token",
                    "new-refresh-token",
                    3600,
                    86400,
                    "Bearer"
            );

            when(authService.refreshToken(refreshToken))
                    .thenReturn(tokenResponse);

            when(refreshTokenProperties.expiration()).thenReturn(86400000L);
            when(cookieProperties.httpOnly()).thenReturn(true);
            when(cookieProperties.secure()).thenReturn(false);
            when(cookieProperties.path()).thenReturn("/");
            when(cookieProperties.sameSite()).thenReturn("Lax");

            when(messageSource.getMessage(eq("success.auth.tokenRefreshed"), any(), any()))
                    .thenReturn("Token refreshed successfully");

            // When & Then
            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refresh_token", refreshToken)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access_token").value("new-access-token"))
                    .andExpect(jsonPath("$.data.expires_in").value(3600))
                    .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                    .andExpect(header().exists("Set-Cookie"));

            verify(authService).refreshToken(refreshToken);
        }

        @Test
        @DisplayName("Should fail without refresh token cookie")
        void shouldFailWithoutRefreshTokenCookie() throws Exception {
            // When & Then
            mockMvc.perform(post("/auth/refresh"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());

            verify(authService, never()).refreshToken(any());
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout user successfully")
        void shouldLogoutUserSuccessfully() throws Exception {
            // Given
            String refreshToken = "refresh-token";

            when(cookieProperties.httpOnly()).thenReturn(true);
            when(cookieProperties.secure()).thenReturn(false);
            when(cookieProperties.path()).thenReturn("/");
            when(cookieProperties.sameSite()).thenReturn("Lax");

            when(messageSource.getMessage(eq("success.auth.logoutSuccessful"), any(), any()))
                    .thenReturn("Logout successful");

            doNothing().when(authService).logout(refreshToken);

            // When & Then
            mockMvc.perform(post("/auth/logout")
                            .cookie(new Cookie("refresh_token", refreshToken)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logout successful"))
                    .andExpect(header().exists("Set-Cookie"))
                    .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

            verify(authService).logout(refreshToken);
        }

        @Test
        @DisplayName("Should logout even without refresh token")
        void shouldLogoutEvenWithoutRefreshToken() throws Exception {
            // Given
            when(cookieProperties.httpOnly()).thenReturn(true);
            when(cookieProperties.secure()).thenReturn(false);
            when(cookieProperties.path()).thenReturn("/");
            when(cookieProperties.sameSite()).thenReturn("Lax");

            when(messageSource.getMessage(eq("success.auth.logoutSuccessful"), any(), any()))
                    .thenReturn("Logout successful");

            // When & Then
            mockMvc.perform(post("/auth/logout"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logout successful"));

            verify(authService, never()).logout(any());
        }
    }

    @Nested
    @DisplayName("Forgot Password Tests")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should initiate password reset successfully")
        void shouldInitiatePasswordResetSuccessfully() throws Exception {
            // Given
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("test@example.com");

            when(messageSource.getMessage(eq("success.auth.resetEmailSent"), any(), any()))
                    .thenReturn("Password reset email sent");

            doNothing().when(authService).initiatePasswordReset(any(ForgotPasswordRequestDto.class));

            // When & Then
            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset email sent"));

            verify(authService).initiatePasswordReset(any(ForgotPasswordRequestDto.class));
        }

        @Test
        @DisplayName("Should fail with invalid email")
        void shouldFailWithInvalidEmail() throws Exception {
            // Given
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("invalid-email");

            // When & Then
            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).initiatePasswordReset(any());
        }
    }

    @Nested
    @DisplayName("Reset Password Tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password successfully")
        void shouldResetPasswordSuccessfully() throws Exception {
            // Given
            ResetPasswordRequestDto request = new ResetPasswordRequestDto(
                    "reset-token",
                    "NewPassword123!"
            );

            when(messageSource.getMessage(eq("success.auth.passwordReset"), any(), any()))
                    .thenReturn("Password reset successfully");

            doNothing().when(authService).resetPassword(any(ResetPasswordRequestDto.class));

            // When & Then
            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset successfully"));

            verify(authService).resetPassword(any(ResetPasswordRequestDto.class));
        }
    }

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() throws Exception {
            // Given
            ChangePasswordRequestDto request = new ChangePasswordRequestDto(
                    "currentPassword123",
                    "NewPassword123!"
            );

            UUID userId = UUID.randomUUID();
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .subject(userId.toString())
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, Collections.emptyList());
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(messageSource.getMessage(eq("success.auth.passwordChanged"), any(), any()))
                    .thenReturn("Password changed successfully");

            doNothing().when(authService).changePassword(any(UUID.class), any(ChangePasswordRequestDto.class));

            // When & Then
            mockMvc.perform(put("/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));

            verify(authService).changePassword(any(UUID.class), any(ChangePasswordRequestDto.class));
        }
    }

    @Nested
    @DisplayName("Change Email Tests")
    class ChangeEmailTests {

        @Test
        @DisplayName("Should change email successfully")
        void shouldChangeEmailSuccessfully() throws Exception {
            // Given
            ChangeEmailRequestDto request = new ChangeEmailRequestDto("newemail@example.com");

            UUID userId = UUID.randomUUID();
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .subject(userId.toString())
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, Collections.emptyList());
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(messageSource.getMessage(eq("success.auth.emailChangeInitiated"), any(), any()))
                    .thenReturn("Email change initiated");

            doNothing().when(authService).changeEmail(any(UUID.class), any(ChangeEmailRequestDto.class));

            // When & Then
            mockMvc.perform(put("/auth/change-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Email change initiated"));

            verify(authService).changeEmail(any(UUID.class), any(ChangeEmailRequestDto.class));
        }

        @Test
        @DisplayName("Should fail with invalid email")
        void shouldFailWithInvalidEmail() throws Exception {
            // Given
            ChangeEmailRequestDto request = new ChangeEmailRequestDto("invalid-email");

            // When & Then
            mockMvc.perform(put("/auth/change-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(authService, never()).changeEmail(any(), any());
        }
    }
}
