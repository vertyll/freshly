package com.vertyll.freshly.auth.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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

import com.vertyll.freshly.auth.api.dto.*;
import com.vertyll.freshly.auth.domain.VerificationTokenService;
import com.vertyll.freshly.auth.domain.event.UserRegisteredEvent;
import com.vertyll.freshly.auth.keycloak.KeycloakAdminClient;
import com.vertyll.freshly.auth.keycloak.KeycloakTokenClient;
import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.notification.application.NotificationService;
import com.vertyll.freshly.useraccess.application.UserAccessService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String FRONTEND_URL = "http://localhost:4200";
    private static final String BEARER_TOKEN_TYPE = "Bearer";
    private static final String VERIFICATION_TOKEN = "verification-token";

    private static final String DATABASE_ERROR = "Database error";

    @Mock private KeycloakAdminClient keycloakAdminClient;
    @Mock private KeycloakTokenClient keycloakTokenClient;
    @Mock private UserAccessService userAccessService;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private VerificationTokenService verificationTokenService;

    @InjectMocks private AuthService authService;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
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
            RegisterUserRequestDto request =
                    new RegisterUserRequestDto(
                            "testuser", "test@example.com", "password123", "John", "Doe");

            UUID keycloakUserId = UUID.randomUUID();

            when(keycloakAdminClient.createUser(any(), any(), any(), any(), any()))
                    .thenReturn(keycloakUserId);

            when(verificationTokenService.generateEmailVerificationToken(any(), any()))
                    .thenReturn(VERIFICATION_TOKEN);

            // When
            UUID result = authService.registerUser(request);

            // Then
            assertThat(result).isEqualTo(keycloakUserId);
            verify(userAccessService).createUser(keycloakUserId, false, Set.of(UserRoleEnum.USER));
            verify(notificationService)
                    .sendEmailVerification(eq(request.email()), any(), anyString());

            ArgumentCaptor<UserRegisteredEvent> eventCaptor =
                    ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().userId()).isEqualTo(keycloakUserId);
        }

        @Test
        @DisplayName("Should rollback Keycloak user on failure")
        void shouldRollbackKeycloakUserOnFailure() {
            // Given
            RegisterUserRequestDto request =
                    new RegisterUserRequestDto("user", "e@e.com", "p", "J", "D");
            UUID keycloakUserId = UUID.randomUUID();

            when(keycloakAdminClient.createUser(any(), any(), any(), any(), any()))
                    .thenReturn(keycloakUserId);
            doThrow(new RuntimeException(DATABASE_ERROR))
                    .when(userAccessService)
                    .createUser(any(), anyBoolean(), any());

            // When & Then
            assertThatThrownBy(() -> authService.registerUser(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(DATABASE_ERROR);

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
            String token = "valid-token";
            UUID userId = UUID.randomUUID();
            when(verificationTokenService.validateEmailVerificationToken(token)).thenReturn(userId);

            // When
            authService.verifyEmail(token);

            // Then
            verify(keycloakAdminClient).activateUser(userId);
            verify(userAccessService).activateUser(userId);
        }
    }

    @Nested
    @DisplayName("Login & Token Tests")
    class TokenTests {

        @Test
        @DisplayName("Should login user successfully")
        void shouldLoginUserSuccessfully() {
            // Given
            LoginRequestDto request = new LoginRequestDto("user", "pass");
            TokenResponseDto expected =
                    new TokenResponseDto("at", "rt", 3600, 86400, BEARER_TOKEN_TYPE);
            when(keycloakTokenClient.getToken(any(), any())).thenReturn(expected);

            // When
            TokenResponseDto result = authService.login(request);

            // Then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            String rt = "refresh-token";
            TokenResponseDto expected =
                    new TokenResponseDto("nat", "nrt", 3600, 86400, BEARER_TOKEN_TYPE);
            when(keycloakTokenClient.refreshToken(rt)).thenReturn(expected);

            // When
            TokenResponseDto result = authService.refreshToken(rt);

            // Then
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Password Management Tests")
    class PasswordManagementTests {

        @Test
        @DisplayName("Should initiate password reset successfully")
        void shouldInitiatePasswordResetSuccessfully() {
            // Given
            ForgotPasswordRequestDto request = new ForgotPasswordRequestDto("test@example.com");
            UserRepresentation user = new UserRepresentation();
            user.setId(UUID.randomUUID().toString());
            user.setUsername("testuser");

            when(keycloakAdminClient.findUsersByEmail(request.email())).thenReturn(List.of(user));

            // When
            authService.initiatePasswordReset(request);

            // Then
            verify(verificationTokenService).generatePasswordResetToken(any(), eq(request.email()));
            verify(notificationService)
                    .sendPasswordResetEmail(eq(request.email()), any(), anyString());
        }

        @Test
        @DisplayName("Should reset password successfully")
        void shouldResetPasswordSuccessfully() {
            // Given
            ResetPasswordRequestDto request = new ResetPasswordRequestDto("token", "newPass");
            UUID userId = UUID.randomUUID();
            when(verificationTokenService.validatePasswordResetToken("token")).thenReturn(userId);

            // When
            authService.resetPassword(request);

            // Then
            verify(keycloakAdminClient).changePassword(userId, "newPass");
        }
    }

    @Nested
    @DisplayName("Change Account Details Tests")
    class ChangeAccountTests {

        @Test
        @DisplayName("Should change email and deactivate user")
        void shouldChangeEmailSuccessfully() {
            // Given
            UUID userId = UUID.randomUUID();
            ChangeEmailRequestDto request = new ChangeEmailRequestDto("new@e.com");

            // When
            authService.changeEmail(userId, request);

            // Then
            verify(keycloakAdminClient).changeEmail(userId, "new@e.com");
            verify(userAccessService).deactivateUser(userId, userId);
            verify(notificationService).sendEmailVerification(eq("new@e.com"), any(), anyString());
        }
    }
}
