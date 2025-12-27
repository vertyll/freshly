package com.vertyll.freshly.auth.application;

import com.vertyll.freshly.auth.domain.VerificationTokenService;
import com.vertyll.freshly.auth.domain.event.UserRegisteredEvent;
import com.vertyll.freshly.notification.application.NotificationService;
import com.vertyll.freshly.security.keycloak.KeycloakAdminClient;
import com.vertyll.freshly.useraccess.application.UserAccessService;
import com.vertyll.freshly.useraccess.domain.UserRoleEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserAccessService userAccessService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final VerificationTokenService verificationTokenService;

    @Value("${application.frontend.url}")
    private String frontendUrl;

    @Transactional
    public UUID registerUser(
            String username,
            String email,
            String password,
            String firstName,
            String lastName
    ) {

        log.info("Registering user: {}", username);

        UUID keycloakUserId = keycloakAdminClient.createUser(
                username, email, password, firstName, lastName
        );

        try {
            userAccessService.createUser(
                    keycloakUserId,
                    false, // Inactive until email verified
                    Set.of(UserRoleEnum.USER)
            );

            String verificationToken = verificationTokenService.generateEmailVerificationToken(
                    keycloakUserId, email
            );
            String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

            notificationService.sendEmailVerification(email, username, verificationLink);

            eventPublisher.publishEvent(new UserRegisteredEvent(keycloakUserId, username, email));

            log.info("User registered successfully: {}", username);
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

    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        String username = keycloakAdminClient.getUsernameById(userId);

        keycloakAdminClient.verifyPassword(username, currentPassword);

        keycloakAdminClient.changePassword(userId, newPassword);

        log.info("Password changed successfully for user: {}", userId);
    }

    @Transactional
    public void changeEmail(UUID userId, String newEmail) {
        keycloakAdminClient.changeEmail(userId, newEmail);
        userAccessService.deactivateUser(userId, userId);

        String verificationToken = verificationTokenService.generateEmailVerificationToken(
                userId, newEmail
        );
        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

        notificationService.sendEmailVerification(newEmail, "User", verificationLink);

        log.info("Email change initiated for user: {}", userId);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        // TODO: Implement password reset flow
        log.info("Password reset initiated for email: {}", email);
    }
}
