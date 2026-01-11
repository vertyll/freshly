package com.vertyll.freshly.notification.application;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.notification.application.dto.SendEmailCommand;
import com.vertyll.freshly.notification.domain.Email;
import com.vertyll.freshly.notification.domain.EmailNotification;
import com.vertyll.freshly.notification.domain.EmailSender;
import com.vertyll.freshly.notification.domain.exception.EmailSendingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailSender emailSender;

    public void sendEmail(SendEmailCommand command) {
        log.info(
                "Sending email to: {} using template: {}",
                command.recipientEmail(),
                command.template());

        Email recipient = new Email(command.recipientEmail());

        EmailNotification notification =
                new EmailNotification(recipient, command.template(), command.templateVariables());

        try {
            emailSender.send(notification);
            notification.markAsSent();
            log.info("Email sent successfully to: {}", recipient);

        } catch (EmailSendingException e) {
            notification.markAsFailed(e.getMessage());
            log.error("Failed to send email to: {}", recipient, e);
            throw e;
        }
    }

    public void sendEmailVerification(String email, String username, String verificationLink) {
        sendEmail(
                new SendEmailCommand(
                        email,
                        com.vertyll.freshly.notification.domain.EmailTemplate.EMAIL_VERIFICATION,
                        java.util.Map.of(
                                "username", username,
                                "verificationLink", verificationLink)));
    }

    public void sendWelcomeEmail(String email, String username) {
        sendEmail(
                new SendEmailCommand(
                        email,
                        com.vertyll.freshly.notification.domain.EmailTemplate.USER_REGISTERED,
                        java.util.Map.of("username", username)));
    }

    public void sendPasswordResetEmail(String email, String username, String resetLink) {
        sendEmail(
                new SendEmailCommand(
                        email,
                        com.vertyll.freshly.notification.domain.EmailTemplate.PASSWORD_RESET,
                        java.util.Map.of(
                                "username", username,
                                "resetLink", resetLink)));
    }
}
