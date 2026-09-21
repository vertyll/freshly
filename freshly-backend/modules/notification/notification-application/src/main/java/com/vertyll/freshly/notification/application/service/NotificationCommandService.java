package com.vertyll.freshly.notification.application.service;

import java.util.Map;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.UseCaseLogger;
import com.vertyll.freshly.notification.application.command.SendEmailCommand;
import com.vertyll.freshly.notification.application.port.inbound.NotificationCommandUseCase;
import com.vertyll.freshly.notification.application.port.outbound.EmailDispatchPort;
import com.vertyll.freshly.notification.domain.model.DeliveryStatus;
import com.vertyll.freshly.notification.domain.model.Email;
import com.vertyll.freshly.notification.domain.model.EmailNotification;
import com.vertyll.freshly.notification.domain.model.EmailTemplate;

public class NotificationCommandService implements NotificationCommandUseCase {
    private static final String USERNAME = "username";
    private static final String VERIFICATION_LINK = "verificationLink";
    private static final String RESET_LINK = "resetLink";

    private final EmailDispatchPort dispatch;
    private final UseCaseLogger logger;

    public NotificationCommandService(EmailDispatchPort dispatch, UseCaseLogger logger) {
        this.dispatch = dispatch;
        this.logger = logger;
    }

    @Override
    public void send(SendEmailCommand command) {
        EmailNotification notification = EmailNotification.create(
            new Email(command.recipientEmail()),
            command.template(),
            command.templateVariables(),
            command.languageTag()
        );

        try {
            dispatch.dispatch(notification);
            notification.markAsSent();
            logger.info("Sent {} to {}", notification.template(), notification.recipient());
        } catch (DomainException e) {
            if (notification.status() != DeliveryStatus.SENT) {
                notification.markAsFailed(e.getMessage());
            }
            logger.error("Failed to send {} to {}", notification.template(), notification.recipient(), e);
            throw e;
        }
    }

    @Override
    public void sendEmailVerification(
        String recipientEmail,
        String username,
        String verificationLink,
        String languageTag
    ) {
        send(
            new SendEmailCommand(
                recipientEmail,
                EmailTemplate.EMAIL_VERIFICATION,
                Map.of(USERNAME, username, VERIFICATION_LINK, verificationLink),
                languageTag
            )
        );
    }

    @Override
    public void sendWelcomeEmail(String recipientEmail, String username, String languageTag) {
        send(
            new SendEmailCommand(recipientEmail, EmailTemplate.USER_REGISTERED, Map.of(USERNAME, username), languageTag)
        );
    }

    @Override
    public void sendPasswordReset(String recipientEmail, String username, String resetLink, String languageTag) {
        send(
            new SendEmailCommand(
                recipientEmail,
                EmailTemplate.PASSWORD_RESET,
                Map.of(USERNAME, username, RESET_LINK, resetLink),
                languageTag
            )
        );
    }
}
