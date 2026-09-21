package com.vertyll.freshly.auth.application.service.command;

import java.util.Map;
import java.util.Optional;

import com.vertyll.freshly.auth.application.command.ChangeEmailCommand;
import com.vertyll.freshly.auth.application.command.ChangePasswordCommand;
import com.vertyll.freshly.auth.application.command.ResetPasswordCommand;
import com.vertyll.freshly.auth.application.port.inbound.command.CredentialsUseCase;
import com.vertyll.freshly.auth.application.port.outbound.IdentityProviderPort;
import com.vertyll.freshly.auth.application.port.outbound.UserNotificationPort;
import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.auth.application.port.outbound.VerificationLinkFactory;
import com.vertyll.freshly.auth.application.port.outbound.VerificationTokenPort;
import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.auth.domain.model.TokenPurpose;
import com.vertyll.freshly.auth.domain.model.VerificationToken;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.UseCaseLogger;

public class CredentialsService implements CredentialsUseCase {
    private static final String USER_ID = "userId";

    private final IdentityProviderPort identityProvider;
    private final UserProvisioningPort provisioning;
    private final UserNotificationPort notifications;
    private final VerificationTokenPort tokens;
    private final VerificationLinkFactory links;
    private final UseCaseLogger logger;

    @SuppressWarnings("java:S107")
    public CredentialsService(
        IdentityProviderPort identityProvider,
        UserProvisioningPort provisioning,
        UserNotificationPort notifications,
        VerificationTokenPort tokens,
        VerificationLinkFactory links,
        UseCaseLogger logger
    ) {
        this.identityProvider = identityProvider;
        this.provisioning = provisioning;
        this.notifications = notifications;
        this.tokens = tokens;
        this.links = links;
        this.logger = logger;
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        IdentityProviderPort.IdentityUser user = identityProvider.findById(command.userId())
            .orElseThrow(() -> new DomainException(AuthError.USER_NOT_FOUND, Map.of(USER_ID, command.userId())));

        if (!identityProvider.verifyPassword(user.username(), command.currentPassword())) {
            throw new DomainException(AuthError.CURRENT_PASSWORD_INCORRECT);
        }

        identityProvider.changePassword(command.userId(), command.newPassword());
        logger.info("Password changed for user {}", command.userId());
    }

    @Override
    public void initiatePasswordReset(String email, String languageTag) {
        Optional<IdentityProviderPort.IdentityUser> user = identityProvider.findByEmail(email);

        if (user.isEmpty()) {
            logger.warn("Password reset requested for an address with no account");
            return;
        }

        String token = tokens.issue(user.get().id(), email, TokenPurpose.PASSWORD_RESET);
        notifications.sendPasswordReset(email, user.get().username(), links.passwordResetLink(token), languageTag);

        logger.info("Password reset link sent for user {}", user.get().id());
    }

    @Override
    public void resetPassword(ResetPasswordCommand command) {
        VerificationToken verified = tokens.validate(command.token(), TokenPurpose.PASSWORD_RESET);

        identityProvider.changePassword(verified.subject(), command.newPassword());
        logger.info("Password reset for user {}", verified.subject());
    }

    @Override
    public void changeEmail(ChangeEmailCommand command) {
        IdentityProviderPort.IdentityUser user = identityProvider.findById(command.userId())
            .orElseThrow(() -> new DomainException(AuthError.USER_NOT_FOUND, Map.of(USER_ID, command.userId())));

        identityProvider.changeEmail(command.userId(), command.newEmail());
        provisioning.deactivate(command.userId());

        String token = tokens.issue(command.userId(), command.newEmail(), TokenPurpose.EMAIL_VERIFICATION);

        notifications.sendEmailVerification(
            command.newEmail(),
            user.username(),
            links.emailVerificationLink(token),
            command.languageTag()
        );

        logger.info("Email change initiated for user {}", command.userId());
    }
}
