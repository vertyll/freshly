package com.vertyll.freshly.auth.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.vertyll.freshly.auth.api.dto.ChangeEmailRequestDto;
import com.vertyll.freshly.auth.api.dto.ChangePasswordRequestDto;
import com.vertyll.freshly.auth.api.dto.ForgotPasswordRequestDto;
import com.vertyll.freshly.auth.api.dto.LoginRequestDto;
import com.vertyll.freshly.auth.api.dto.RegisterUserRequestDto;
import com.vertyll.freshly.auth.api.dto.ResetPasswordRequestDto;
import com.vertyll.freshly.auth.api.dto.TokenResponseDto;
import com.vertyll.freshly.auth.domain.VerificationTokenService;
import com.vertyll.freshly.auth.domain.event.UserRegisteredEvent;
import com.vertyll.freshly.auth.keycloak.KeycloakAdminClient;
import com.vertyll.freshly.auth.keycloak.KeycloakTokenClient;
import com.vertyll.freshly.common.config.ApplicationProperties;
import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.notification.application.NotificationService;
import com.vertyll.freshly.useraccess.application.UserAccessService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    @SuppressWarnings("NullAway.Init")
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private KeycloakTokenClient keycloakTokenClient;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private UserAccessService userAccessService;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private NotificationService notificationService;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private ApplicationEventPublisher eventPublisher;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private VerificationTokenService verificationTokenService;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private ApplicationProperties applicationProperties;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private ApplicationProperties.Frontend frontendProperties;

    private AuthService authService;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        authService =
                new AuthService(
                        keycloakAdminClient,
                        keycloakTokenClient,
                        userAccessService,
                        notificationService,
                        eventPublisher,
                        verificationTokenService,
                        applicationProperties);
    }

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        RegisterUserRequestDto request =
                new RegisterUserRequestDto(
                        "testuser", "test@example.com", "Password1!", "Jan", "Kowalski");

        when(keycloakAdminClient.createUser(
                        request.username(),
                        request.email(),
                        request.password(),
                        request.firstName(),
                        request.lastName()))
                .thenReturn(keycloakUserId);
        when(applicationProperties.frontend()).thenReturn(frontendProperties);
        when(frontendProperties.url()).thenReturn("https://app.example.com");
        when(verificationTokenService.generateEmailVerificationToken(
                        keycloakUserId, request.email()))
                .thenReturn("verify-token");

        // When
        UUID result = authService.registerUser(request);

        // Then
        assertThat(result).isEqualTo(keycloakUserId);
        verify(keycloakAdminClient)
                .createUser(
                        request.username(),
                        request.email(),
                        request.password(),
                        request.firstName(),
                        request.lastName());
        verify(userAccessService)
                .createUser(keycloakUserId, false, Set.of(UserRoleEnum.USER.getValue()));
        verify(verificationTokenService)
                .generateEmailVerificationToken(keycloakUserId, request.email());
        verify(notificationService)
                .sendEmailVerification(eq(request.email()), eq(request.username()), anyString());
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    @DisplayName("Should create user as inactive during registration")
    void shouldCreateUserAsInactiveDuringRegistration() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        RegisterUserRequestDto request =
                new RegisterUserRequestDto(
                        "testuser", "test@example.com", "Password1!", "Jan", "Kowalski");

        when(keycloakAdminClient.createUser(
                        anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(keycloakUserId);
        when(applicationProperties.frontend()).thenReturn(frontendProperties);
        when(frontendProperties.url()).thenReturn("https://app.example.com");
        when(verificationTokenService.generateEmailVerificationToken(any(), anyString()))
                .thenReturn("verify-token");

        // When
        authService.registerUser(request);

        // Then
        verify(userAccessService)
                .createUser(keycloakUserId, false, Set.of(UserRoleEnum.USER.getValue()));
    }

    @Test
    @DisplayName("Should assign USER role during registration")
    void shouldAssignUserRoleDuringRegistration() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        RegisterUserRequestDto request =
                new RegisterUserRequestDto(
                        "testuser", "test@example.com", "Password1!", "Jan", "Kowalski");

        when(keycloakAdminClient.createUser(
                        anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(keycloakUserId);
        when(applicationProperties.frontend()).thenReturn(frontendProperties);
        when(frontendProperties.url()).thenReturn("https://app.example.com");
        when(verificationTokenService.generateEmailVerificationToken(any(), anyString()))
                .thenReturn("verify-token");

        // When
        authService.registerUser(request);

        // Then
        verify(userAccessService)
                .createUser(keycloakUserId, false, Set.of(UserRoleEnum.USER.getValue()));
        verifyNoMoreInteractions(userAccessService);
    }

    @Test
    @DisplayName("Should rollback Keycloak user when registration fails")
    void shouldRollbackKeycloakUserWhenRegistrationFails() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        RegisterUserRequestDto request =
                new RegisterUserRequestDto(
                        "testuser", "test@example.com", "Password1!", "Jan", "Kowalski");

        when(keycloakAdminClient.createUser(
                        anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(keycloakUserId);
        doThrow(new RuntimeException("DB error"))
                .when(userAccessService)
                .createUser(any(), anyBoolean(), any());

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(RuntimeException.class);
        verify(keycloakAdminClient).deleteUser(keycloakUserId);
    }

    @Test
    @DisplayName("Should send verification email with correct link during registration")
    void shouldSendVerificationEmailWithCorrectLinkDuringRegistration() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        RegisterUserRequestDto request =
                new RegisterUserRequestDto(
                        "testuser", "test@example.com", "Password1!", "Jan", "Kowalski");

        when(keycloakAdminClient.createUser(
                        anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(keycloakUserId);
        when(applicationProperties.frontend()).thenReturn(frontendProperties);
        when(frontendProperties.url()).thenReturn("https://app.example.com");
        when(verificationTokenService.generateEmailVerificationToken(any(), anyString()))
                .thenReturn("verify-token");

        // When
        authService.registerUser(request);

        // Then
        verify(notificationService)
                .sendEmailVerification(
                        "test@example.com",
                        "testuser",
                        "https://app.example.com/verify-email?token=verify-token");
    }

    @Test
    @DisplayName("Should verify email and activate user")
    void shouldVerifyEmailAndActivateUser() {
        // Given
        UUID userId = UUID.randomUUID();
        String token = "valid-token";

        when(verificationTokenService.validateEmailVerificationToken(token)).thenReturn(userId);

        // When
        authService.verifyEmail(token);

        // Then
        verify(verificationTokenService).validateEmailVerificationToken(token);
        verify(keycloakAdminClient).activateUser(userId);
        verify(userAccessService).activateUser(userId, null);
    }

    @Test
    @DisplayName("Should return tokens on successful login")
    void shouldReturnTokensOnSuccessfulLogin() {
        // Given
        LoginRequestDto request = new LoginRequestDto("testuser", "Password1!");
        TokenResponseDto expectedTokens =
                new TokenResponseDto("access-token", "refresh-token", 300, 3600, "Bearer");

        when(keycloakTokenClient.getToken(request.username(), request.password()))
                .thenReturn(expectedTokens);

        // When
        TokenResponseDto result = authService.login(request);

        // Then
        assertThat(result).isEqualTo(expectedTokens);
        verify(keycloakTokenClient).getToken(request.username(), request.password());
    }

    @Test
    @DisplayName("Should return new tokens on refresh")
    void shouldReturnNewTokensOnRefresh() {
        // Given
        String refreshToken = "old-refresh-token";
        TokenResponseDto expectedTokens =
                new TokenResponseDto("new-access-token", "new-refresh-token", 300, 3600, "Bearer");

        when(keycloakTokenClient.refreshToken(refreshToken)).thenReturn(expectedTokens);

        // When
        TokenResponseDto result = authService.refreshToken(refreshToken);

        // Then
        assertThat(result).isEqualTo(expectedTokens);
        verify(keycloakTokenClient).refreshToken(refreshToken);
    }

    @Test
    @DisplayName("Should logout and revoke refresh token")
    void shouldLogoutAndRevokeRefreshToken() {
        // Given
        String refreshToken = "valid-refresh-token";

        // When
        authService.logout(refreshToken);

        // Then
        verify(keycloakTokenClient).logout(refreshToken);
    }

    @Test
    @DisplayName("Should skip Keycloak revocation when refresh token is null")
    void shouldSkipKeycloakRevocationWhenRefreshTokenIsNull() {
        // When
        authService.logout(null);

        // Then
        verifyNoInteractions(keycloakTokenClient);
    }

    @Test
    @DisplayName("Should skip Keycloak revocation when refresh token is blank")
    void shouldSkipKeycloakRevocationWhenRefreshTokenIsBlank() {
        // When
        authService.logout("   ");

        // Then
        verifyNoInteractions(keycloakTokenClient);
    }

    @Test
    @DisplayName("Should send password reset email when user exists")
    void shouldSendPasswordResetEmailWhenUserExists() {
        // Given
        String email = "test@example.com";
        UUID userId = UUID.randomUUID();
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto(email);

        UserRepresentation user = new UserRepresentation();
        user.setId(userId.toString());
        user.setUsername("testuser");

        when(keycloakAdminClient.findUsersByEmail(email)).thenReturn(List.of(user));
        when(applicationProperties.frontend()).thenReturn(frontendProperties);
        when(frontendProperties.url()).thenReturn("https://app.example.com");
        when(verificationTokenService.generatePasswordResetToken(userId, email))
                .thenReturn("reset-token");

        // When
        authService.initiatePasswordReset(request);

        // Then
        verify(verificationTokenService).generatePasswordResetToken(userId, email);
        verify(notificationService)
                .sendPasswordResetEmail(
                        email,
                        "testuser",
                        "https://app.example.com/reset-password?token=reset-token");
    }

    @Test
    @DisplayName("Should silently skip password reset when user does not exist")
    void shouldSilentlySkipPasswordResetWhenUserDoesNotExist() {
        // Given
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("unknown@example.com");

        when(keycloakAdminClient.findUsersByEmail("unknown@example.com")).thenReturn(List.of());

        // When
        authService.initiatePasswordReset(request);

        // Then
        verifyNoInteractions(verificationTokenService);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should reset password with valid token")
    void shouldResetPasswordWithValidToken() {
        // Given
        UUID userId = UUID.randomUUID();
        ResetPasswordRequestDto request =
                new ResetPasswordRequestDto("reset-token", "NewPassword1!");

        when(verificationTokenService.validatePasswordResetToken(request.token()))
                .thenReturn(userId);

        // When
        authService.resetPassword(request);

        // Then
        verify(verificationTokenService).validatePasswordResetToken(request.token());
        verify(keycloakAdminClient).changePassword(userId, request.newPassword());
    }

    @Test
    @DisplayName("Should change password after verifying current password")
    void shouldChangePasswordAfterVerifyingCurrentPassword() {
        // Given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequestDto request =
                new ChangePasswordRequestDto("OldPassword1!", "NewPassword1!");

        when(keycloakAdminClient.getUsernameById(userId)).thenReturn("testuser");

        // When
        authService.changePassword(userId, request);

        // Then
        verify(keycloakAdminClient).getUsernameById(userId);
        verify(keycloakAdminClient).verifyPassword("testuser", request.currentPassword());
        verify(keycloakAdminClient).changePassword(userId, request.newPassword());
    }

    @Test
    @DisplayName("Should change email and deactivate user until verification")
    void shouldChangeEmailAndDeactivateUserUntilVerification() {
        // Given
        UUID userId = UUID.randomUUID();
        ChangeEmailRequestDto request = new ChangeEmailRequestDto("new@example.com");

        when(applicationProperties.frontend()).thenReturn(frontendProperties);
        when(frontendProperties.url()).thenReturn("https://app.example.com");
        when(verificationTokenService.generateEmailVerificationToken(userId, request.newEmail()))
                .thenReturn("verify-token");

        // When
        authService.changeEmail(userId, request);

        // Then
        verify(keycloakAdminClient).changeEmail(userId, request.newEmail());
        verify(userAccessService).deactivateUser(userId, userId, null);
        verify(verificationTokenService).generateEmailVerificationToken(userId, request.newEmail());
        verify(notificationService)
                .sendEmailVerification(eq(request.newEmail()), anyString(), anyString());
    }

    @Test
    @DisplayName("Should send verification email with correct link after email change")
    void shouldSendVerificationEmailWithCorrectLinkAfterEmailChange() {
        // Given
        UUID userId = UUID.randomUUID();
        ChangeEmailRequestDto request = new ChangeEmailRequestDto("new@example.com");

        when(applicationProperties.frontend()).thenReturn(frontendProperties);
        when(frontendProperties.url()).thenReturn("https://app.example.com");
        when(verificationTokenService.generateEmailVerificationToken(any(), anyString()))
                .thenReturn("verify-token");

        // When
        authService.changeEmail(userId, request);

        // Then
        verify(notificationService)
                .sendEmailVerification(
                        eq("new@example.com"),
                        anyString(),
                        eq("https://app.example.com/verify-email?token=verify-token"));
    }
}
