package com.vertyll.freshly.auth.application;

import com.vertyll.freshly.auth.api.dto.*;
import com.vertyll.freshly.auth.domain.VerificationTokenService;
import com.vertyll.freshly.auth.domain.event.UserRegisteredEvent;
import com.vertyll.freshly.notification.application.NotificationService;
import com.vertyll.freshly.security.keycloak.KeycloakAdminClient;
import com.vertyll.freshly.security.keycloak.KeycloakTokenClient;
import com.vertyll.freshly.useraccess.application.UserAccessService;
import com.vertyll.freshly.useraccess.domain.UserRoleEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakTokenClient keycloakTokenClient;
    private final UserAccessService userAccessService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final VerificationTokenService verificationTokenService;

    @Value("${application.frontend.url}")
    private String frontendUrl;

    @Transactional
    @SuppressWarnings("PMD.AvoidCatchingGenericException") // Catch-all before compensation rollback
    public UUID registerUser(RegisterUserRequestDto request) {

        log.info("Registering user: {}", request.username());

        UUID keycloakUserId = keycloakAdminClient.createUser(
                request.username(),
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName()
        );

        try {
            userAccessService.createUser(
                    keycloakUserId,
                    false, // Inactive until email verified
                    Set.of(UserRoleEnum.USER)
            );

            String verificationToken = verificationTokenService.generateEmailVerificationToken(
                    keycloakUserId,
                    request.email()
            );
            String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

            notificationService.sendEmailVerification(
                    request.email(),
                    request.username(),
                    verificationLink
            );

            eventPublisher.publishEvent(new UserRegisteredEvent(
                    keycloakUserId,
                    request.username(),
                    request.email()
            ));

            log.info("User registered successfully: {}", request.username());
            return keycloakUserId;

        } catch (Exception e) {
            log.error("Registration failed, rolling back Keycloak user: {}", keycloakUserId, e);
            try {
                keycloakAdminClient.deleteUser(keycloakUserId);
            } catch (Exception rollbackException) {
                log.error("Failed to rollback Keycloak user: {}", keycloakUserId, rollbackException);
            }
            throw e;
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        UUID userId = verificationTokenService.validateEmailVerificationToken(token);

        keycloakAdminClient.activateUser(userId);
        userAccessService.activateUser(userId);

        log.info("Email verified for user: {}", userId);
    }

    public TokenResponseDto login(LoginRequestDto request) {
        log.info("Processing login for user: {}", request.username());

        // Delegate to Keycloak token endpoint
        return keycloakTokenClient.getToken(request.username(), request.password());
    }

    public TokenResponseDto refreshToken(String refreshToken) {
        log.info("Refreshing access token");

        // Delegate to Keycloak token endpoint
        return keycloakTokenClient.refreshToken(refreshToken);
    }

    public void logout(String refreshToken) {
        log.info("Processing logout");

        // Revoke refresh token in Keycloak
        keycloakTokenClient.logout(refreshToken);
    }

    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequestDto request) {
        log.info("Password reset initiated for email: {}", request.email());

        List<UserRepresentation> users = keycloakAdminClient.findUsersByEmail(request.email());

        if (users.isEmpty()) {
            // Security: Don't reveal if email exists
            log.warn("Password reset requested for non-existent email: {}", request.email());
            return;
        }

        UserRepresentation user = users.getFirst();
        UUID userId = UUID.fromString(user.getId());
        String username = user.getUsername();

        String resetToken = verificationTokenService.generatePasswordResetToken(userId, request.email());
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        notificationService.sendPasswordResetEmail(request.email(), username, resetLink);

        log.info("Password reset email sent to: {}", request.email());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        log.info("Processing password reset");

        UUID userId = verificationTokenService.validatePasswordResetToken(request.token());

        // Change password in Keycloak
        keycloakAdminClient.changePassword(userId, request.newPassword());

        log.info("Password reset successful for user: {}", userId);
    }

    public void changePassword(UUID userId, ChangePasswordRequestDto request) {
        String username = keycloakAdminClient.getUsernameById(userId);

        keycloakAdminClient.verifyPassword(username, request.currentPassword());

        keycloakAdminClient.changePassword(userId, request.newPassword());

        log.info("Password changed successfully for user: {}", userId);
    }

    @Transactional
    public void changeEmail(UUID userId, ChangeEmailRequestDto request) {
        keycloakAdminClient.changeEmail(userId, request.newEmail());
        userAccessService.deactivateUser(userId, userId);

        String verificationToken = verificationTokenService.generateEmailVerificationToken(
                userId,
                request.newEmail()
        );
        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

        notificationService.sendEmailVerification(
                request.newEmail(),
                "User",
                verificationLink
        );

        log.info("Email change initiated for user: {}", userId);
    }
}
