package com.vertyll.freshly.notification.application.port.inbound;

import com.vertyll.freshly.notification.application.command.SendEmailCommand;

public interface NotificationCommandUseCase {
    void send(SendEmailCommand command);

    void sendEmailVerification(String recipientEmail, String username, String verificationLink, String languageTag);

    void sendWelcomeEmail(String recipientEmail, String username, String languageTag);

    void sendPasswordReset(String recipientEmail, String username, String resetLink, String languageTag);
}
