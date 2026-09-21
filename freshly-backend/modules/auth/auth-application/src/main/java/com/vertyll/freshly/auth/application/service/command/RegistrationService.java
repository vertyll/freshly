package com.vertyll.freshly.auth.application.service.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.UUID;

import com.vertyll.freshly.auth.application.command.RegisterUserCommand;
import com.vertyll.freshly.auth.application.port.inbound.command.RegistrationUseCase;
import com.vertyll.freshly.auth.application.port.outbound.IdentityProviderPort;
import com.vertyll.freshly.auth.application.port.outbound.UserNotificationPort;
import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.auth.application.port.outbound.VerificationLinkFactory;
import com.vertyll.freshly.auth.application.port.outbound.VerificationTokenPort;
import com.vertyll.freshly.auth.domain.model.TokenPurpose;
import com.vertyll.freshly.auth.domain.model.UserRole;
import com.vertyll.freshly.auth.domain.model.VerificationToken;
import com.vertyll.freshly.lang.logging.UseCaseLogger;

public class RegistrationService implements RegistrationUseCase {
    private final IdentityProviderPort identityProvider;
    private final UserProvisioningPort provisioning;
    private final UserNotificationPort notifications;
    private final VerificationTokenPort tokens;
    private final VerificationLinkFactory links;
    private final UseCaseLogger logger;

    @SuppressWarnings("java:S107")
    public RegistrationService(
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
    public UUID register(RegisterUserCommand command) {
        Deque<Compensation> unwind = new ArrayDeque<>();

        UUID userId = identityProvider.createUser(
            new IdentityProviderPort.NewIdentity(
                command.username(),
                command.email(),
                command.password(),
                command.firstName(),
                command.lastName()
            )
        );
        unwind.push(new Compensation("delete identity " + userId, () -> identityProvider.deleteUser(userId)));

        boolean registered = false;
        try {
            provisioning.provision(userId, Set.of(UserRole.USER.value()), false);
            unwind.push(new Compensation("deprovision user " + userId, () -> provisioning.deprovision(userId)));

            String token = tokens.issue(userId, command.email(), TokenPurpose.EMAIL_VERIFICATION);

            notifications.sendEmailVerification(
                command.email(),
                command.username(),
                links.emailVerificationLink(token),
                command.languageTag()
            );

            notifications.sendWelcomeEmail(command.email(), command.username(), command.languageTag());

            logger.info("Registered user {} as {}", command.username(), userId);
            registered = true;
            return userId;
        } finally {
            if (!registered) {
                logger.error("Registration failed for {}, compensating", command.username());
                compensate(unwind);
            }
        }
    }

    @Override
    public void verifyEmail(String token) {
        VerificationToken verified = tokens.validate(token, TokenPurpose.EMAIL_VERIFICATION);

        identityProvider.enableUser(verified.subject());
        provisioning.activate(verified.subject());

        logger.info("Verified email for user {}", verified.subject());
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void compensate(Deque<Compensation> unwind) {
        while (!unwind.isEmpty()) {
            Compensation compensation = unwind.pop();
            try {
                compensation.action().run();
                logger.info("Compensated: {}", compensation.description());
            } catch (RuntimeException e) {
                logger.error("Compensation failed: {}", compensation.description(), e);
            }
        }
    }

    private record Compensation(String description, Runnable action) {
    }
}
