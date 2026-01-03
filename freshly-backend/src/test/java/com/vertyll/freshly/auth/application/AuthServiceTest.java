package com.vertyll.freshly.auth.application;

import com.vertyll.freshly.auth.api.dto.*;
import com.vertyll.freshly.auth.domain.VerificationTokenService;
import com.vertyll.freshly.auth.domain.event.UserRegisteredEvent;
import com.vertyll.freshly.notification.application.NotificationService;
import com.vertyll.freshly.security.keycloak.KeycloakAdminClient;
import com.vertyll.freshly.security.keycloak.KeycloakTokenClient;
import com.vertyll.freshly.useraccess.application.UserAccessService;
import com.vertyll.freshly.useraccess.domain.UserRoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private KeycloakTokenClient keycloakTokenClient;

    @Mock
    private UserAccessService userAccessService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private VerificationTokenService verificationTokenService;

    @InjectMocks
    private AuthService authService;

    private static final String FRONTEND_URL = "http://localhost:4200";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendUrl", FRONTEND_URL);
    }

    @Nested
    @DisplayName("User Registration Tests")
    class UserRegistrationTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() {
            // Given
            RegisterUserRequestDto request = new RegisterUserRequestDto(
                    "testuser",
                    "test@example.com",
                    "password123",
                    "John",
                    "Doe"
            );

            UUID keycloakUserId = UUID.randomUUID();
            String verificationToken = "verification-token";

            when(keycloakAdminClient.createUser(
                    request.username(),
                    request.email(),
                    request.password(),
                    request.firstName(),
                    request.lastName()
            )).thenReturn(keycloakUserId);

            when(verificationTokenService.generateEmailVerificationToken(
                    keycloakUserId,
                    request.email()
            )).thenReturn(verificationToken);

            // When
            UUID result = authService.registerUser(request);

            // Then
            assertThat(result).isEqualTo(keycloakUserId);

            verify(keycloakAdminClient).createUser(
                    request.username(),
                    request.email(),
                    request.password(),
                    request.firstName(),
                    request.lastName()
            );

            verify(userAccessService).createUser(
                    keycloakUserId,
                    false,
                    Set.of(UserRoleEnum.USER)
            );

            verify(verificationTokenService).generateEmailVerificationToken(
                    keycloakUserId,
                    request.email()
            );

            verify(notificationService).sendEmailVerification(
                    eq(request.email()),
                    eq(request.username()),
                    contains("verify-email?token=" + verificationToken)
            );

            ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            UserRegisteredEvent event = eventCaptor.getValue();
            assertThat(event.userId()).isEqualTo(keycloakUserId);
            assertThat(event.username()).isEqualTo(request.username());
            assertThat(event.email()).isEqualTo(request.email());
        }

        @Test
        @DisplayName("Should rollback Keycloak user on failure")
        void shouldRollbackKeycloakUserOnFailure() {
            // Given
            RegisterUserRequestDto request = new RegisterUserRequestDto(
                    "testuser",
                    "test@example.com",
                    "password123",
                    "John",
                    "Doe"
            );

            UUID keycloakUserId = UUID.randomUUID();

            when(keycloakAdminClient.createUser(
                    request.username(),
                    request.email(),
                    request.password(),
                    request.firstName(),
                    request.lastName()
            )).thenReturn(keycloakUserId);

            doThrow(new RuntimeException("Database error"))
                    .when(userAccessService).createUser(any(), anyBoolean(), any());

            // When & Then
            assertThatThrownBy(() -> authService.registerUser(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");

            verify(keycloakAdminClient).deleteUser(keycloakUserId);
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email successfully")
        void shouldVerifyEmailSuccessfully() {
            // Given
            String token = "verification-token";
            UUID userId = UUID.randomUUID();

            when(verificationTokenService.validateEmailVerificationToken(token))
                    .thenReturn(userId);

            // When
            authService.verifyEmail(token);

            // Then
            verify(verificationTokenService).validateEmailVerificationToken(token);
            verify(keycloakAdminClient).activateUser(userId);
            verify(userAccessService).activateUser(userId);
        }

        @Test
        @DisplayName("Should propagate validation exception")
        void shouldPropagateValidationException() {
            // Given
            String token = "invalid-token";

            when(verificationTokenService.validateEmailVerificationToken(token))
                    .thenThrow(new RuntimeException("Invalid token"));

            // When & Then
            assertThatThrownBy(() -> authService.verifyEmail(token))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid token");

            verifyNoInteractions(keycloakAdminClient);
            verifyNoInteractions(userAccessService);
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully")
        void shouldLoginUserSuccessfully() {
            // Given
            LoginRequestDto request = new LoginRequestDto("testuser", "password123");
            TokenResponseDto expectedResponse = new TokenResponseDto(
                    "access-token",
                    "refresh-token",
                    3600,
                    86400,
                    "Bearer"
            );

            when(keycloakTokenClient.getToken(request.username(), request.password()))
                    .thenReturn(expectedResponse);

            // When
            TokenResponseDto result = authService.login(request);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(keycloakTokenClient).getToken(request.username(), request.password());
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    class TokenRefreshTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            String refreshToken = "refresh-token";
            TokenResponseDto expectedResponse = new TokenResponseDto(
                    "new-access-token",
                    "new-refresh-token",
                    3600,
                    86400,
                    "Bearer"
            );

            when(keycloakTokenClient.refreshToken(refreshToken))
                    .thenReturn(expectedResponse);

            // When
            TokenResponseDto result = authService.refreshToken(refreshToken);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(keycloakTokenClient).refreshToken(refreshToken);
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout user successfully")
        void shouldLogoutUserSuccessfully() {
            // Given
            String refreshToken = "refresh-token";

            // When
            authService.logout(refreshToken);

            // Then
            verify(keycloakTokenClient).logout(refreshToken);
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Should initiate password reset successfully")
        void shouldInitiatePasswordResetSuccessfully() {
            // Given
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("test@example.com");
            UUID userId = UUID.randomUUID();
            String username = "testuser";
            String resetToken = "reset-token";

            UserRepresentation user = new UserRepresentation();
            user.setId(userId.toString());
            user.setUsername(username);

            when(keycloakAdminClient.findUsersByEmail(request.email()))
                    .thenReturn(List.of(user));

            when(verificationTokenService.generatePasswordResetToken(userId, request.email()))
                    .thenReturn(resetToken);

            // When
            authService.initiatePasswordReset(request);

            // Then
            verify(keycloakAdminClient).findUsersByEmail(request.email());
            verify(verificationTokenService).generatePasswordResetToken(userId, request.email());
            verify(notificationService).sendPasswordResetEmail(
                    eq(request.email()),
                    eq(username),
                    contains("reset-password?token=" + resetToken)
            );
        }

        @Test
        @DisplayName("Should not reveal non-existent email")
        void shouldNotRevealNonExistentEmail() {
            // Given
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("nonexistent@example.com");

            when(keycloakAdminClient.findUsersByEmail(request.email()))
                    .thenReturn(List.of());

            // When
            authService.initiatePasswordReset(request);

            // Then
            verify(keycloakAdminClient).findUsersByEmail(request.email());
            verifyNoInteractions(verificationTokenService);
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("Should reset password successfully")
        void shouldResetPasswordSuccessfully() {
            // Given
            String token = "reset-token";
            String newPassword = "newPassword123";
            ResetPasswordRequestDto request = new ResetPasswordRequestDto(token, newPassword);
            UUID userId = UUID.randomUUID();

            when(verificationTokenService.validatePasswordResetToken(token))
                    .thenReturn(userId);

            // When
            authService.resetPassword(request);

            // Then
            verify(verificationTokenService).validatePasswordResetToken(token);
            verify(keycloakAdminClient).changePassword(userId, newPassword);
        }
    }

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() {
            // Given
            UUID userId = UUID.randomUUID();
            String username = "testuser";
            ChangePasswordRequestDto request = new ChangePasswordRequestDto(
                    "currentPassword",
                    "newPassword123"
            );

            when(keycloakAdminClient.getUsernameById(userId)).thenReturn(username);

            // When
            authService.changePassword(userId, request);

            // Then
            verify(keycloakAdminClient).getUsernameById(userId);
            verify(keycloakAdminClient).verifyPassword(username, request.currentPassword());
            verify(keycloakAdminClient).changePassword(userId, request.newPassword());
        }

        @Test
        @DisplayName("Should fail when current password is incorrect")
        void shouldFailWhenCurrentPasswordIsIncorrect() {
            // Given
            UUID userId = UUID.randomUUID();
            String username = "testuser";
            ChangePasswordRequestDto request = new ChangePasswordRequestDto(
                    "wrongPassword",
                    "newPassword123"
            );

            when(keycloakAdminClient.getUsernameById(userId)).thenReturn(username);
            doThrow(new RuntimeException("Invalid password"))
                    .when(keycloakAdminClient).verifyPassword(username, request.currentPassword());

            // When & Then
            assertThatThrownBy(() -> authService.changePassword(userId, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid password");

            verify(keycloakAdminClient).getUsernameById(userId);
            verify(keycloakAdminClient).verifyPassword(username, request.currentPassword());
            verify(keycloakAdminClient, never()).changePassword(any(), any());
        }
    }

    @Nested
    @DisplayName("Change Email Tests")
    class ChangeEmailTests {

        @Test
        @DisplayName("Should change email successfully")
        void shouldChangeEmailSuccessfully() {
            // Given
            UUID userId = UUID.randomUUID();
            String newEmail = "newemail@example.com";
            ChangeEmailRequestDto request = new ChangeEmailRequestDto(newEmail);
            String verificationToken = "verification-token";

            when(verificationTokenService.generateEmailVerificationToken(userId, newEmail))
                    .thenReturn(verificationToken);

            // When
            authService.changeEmail(userId, request);

            // Then
            verify(keycloakAdminClient).changeEmail(userId, newEmail);
            verify(userAccessService).deactivateUser(userId, userId);
            verify(verificationTokenService).generateEmailVerificationToken(userId, newEmail);
            verify(notificationService).sendEmailVerification(
                    eq(newEmail),
                    eq("User"),
                    contains("verify-email?token=" + verificationToken)
            );
        }
    }
}
