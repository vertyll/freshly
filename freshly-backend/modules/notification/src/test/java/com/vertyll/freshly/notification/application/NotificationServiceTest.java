package com.vertyll.freshly.notification.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vertyll.freshly.notification.application.dto.SendEmailCommand;
import com.vertyll.freshly.notification.domain.EmailNotification;
import com.vertyll.freshly.notification.domain.EmailSender;
import com.vertyll.freshly.notification.domain.EmailTemplate;
import com.vertyll.freshly.notification.domain.exception.EmailSendingException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    @SuppressWarnings("NullAway.Init")
    private EmailSender emailSender;

    @Captor
    @SuppressWarnings("NullAway.Init")
    private ArgumentCaptor<EmailNotification> notificationCaptor;

    private NotificationService notificationService;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        notificationService = new NotificationService(emailSender);
    }

    @Test
    @DisplayName("Should send email successfully")
    void shouldSendEmailSuccessfully() {
        // Given
        String recipientEmail = "test@example.com";
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables =
                Map.of("username", "John", "verificationLink", "https://example.com");
        SendEmailCommand command = new SendEmailCommand(recipientEmail, template, variables);

        doNothing().when(emailSender).send(any(EmailNotification.class));

        // When
        notificationService.sendEmail(command);

        // Then
        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification sentNotification = notificationCaptor.getValue();

        assertThat(sentNotification.getRecipient().value()).isEqualTo(recipientEmail);
        assertThat(sentNotification.getTemplate()).isEqualTo(template);
        assertThat(sentNotification.getTemplateVariables()).isEqualTo(variables);
        assertThat(sentNotification.getStatus()).isEqualTo(EmailNotification.EmailStatus.SENT);
    }

    @Test
    @DisplayName("Should throw exception when email sending fails")
    void shouldThrowExceptionWhenEmailSendingFails() {
        // Given
        String recipientEmail = "test@example.com";
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of("username", "John");
        SendEmailCommand command = new SendEmailCommand(recipientEmail, template, variables);

        EmailSendingException exception = new EmailSendingException("SMTP error");
        doThrow(exception).when(emailSender).send(any(EmailNotification.class));

        // When & Then
        assertThatThrownBy(() -> notificationService.sendEmail(command))
                .isInstanceOf(EmailSendingException.class)
                .hasMessageContaining("SMTP error");

        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification failedNotification = notificationCaptor.getValue();
        assertThat(failedNotification.getStatus()).isEqualTo(EmailNotification.EmailStatus.FAILED);
        assertThat(failedNotification.getErrorMessage()).isEqualTo("SMTP error");
    }

    @Test
    @DisplayName("Should send email verification successfully")
    void shouldSendEmailVerificationSuccessfully() {
        // Given
        String email = "user@example.com";
        String username = "John Doe";
        String verificationLink = "https://example.com/verify/token123";

        doNothing().when(emailSender).send(any(EmailNotification.class));

        // When
        notificationService.sendEmailVerification(email, username, verificationLink);

        // Then
        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification sentNotification = notificationCaptor.getValue();

        assertThat(sentNotification.getRecipient().value()).isEqualTo(email);
        assertThat(sentNotification.getTemplate()).isEqualTo(EmailTemplate.EMAIL_VERIFICATION);
        assertThat(sentNotification.getTemplateVariables()).containsEntry("username", username);
        assertThat(sentNotification.getTemplateVariables())
                .containsEntry("verificationLink", verificationLink);
    }

    @Test
    @DisplayName("Should send welcome email successfully")
    void shouldSendWelcomeEmailSuccessfully() {
        // Given
        String email = "user@example.com";
        String username = "John Doe";

        doNothing().when(emailSender).send(any(EmailNotification.class));

        // When
        notificationService.sendWelcomeEmail(email, username);

        // Then
        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification sentNotification = notificationCaptor.getValue();

        assertThat(sentNotification.getRecipient().value()).isEqualTo(email);
        assertThat(sentNotification.getTemplate()).isEqualTo(EmailTemplate.USER_REGISTERED);
        assertThat(sentNotification.getTemplateVariables()).containsEntry("username", username);
        assertThat(sentNotification.getTemplateVariables()).hasSize(1);
    }

    @Test
    @DisplayName("Should send password reset email successfully")
    void shouldSendPasswordResetEmailSuccessfully() {
        // Given
        String email = "user@example.com";
        String username = "John Doe";
        String resetLink = "https://example.com/reset/token456";

        doNothing().when(emailSender).send(any(EmailNotification.class));

        // When
        notificationService.sendPasswordResetEmail(email, username, resetLink);

        // Then
        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification sentNotification = notificationCaptor.getValue();

        assertThat(sentNotification.getRecipient().value()).isEqualTo(email);
        assertThat(sentNotification.getTemplate()).isEqualTo(EmailTemplate.PASSWORD_RESET);
        assertThat(sentNotification.getTemplateVariables()).containsEntry("username", username);
        assertThat(sentNotification.getTemplateVariables()).containsEntry("resetLink", resetLink);
    }

    @Test
    @DisplayName("Should mark notification as sent after successful sending")
    void shouldMarkNotificationAsSentAfterSuccessfulSending() {
        // Given
        SendEmailCommand command =
                new SendEmailCommand(
                        "test@example.com",
                        EmailTemplate.USER_REGISTERED,
                        Map.of("username", "John"));

        doNothing().when(emailSender).send(any(EmailNotification.class));

        // When
        notificationService.sendEmail(command);

        // Then
        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification notification = notificationCaptor.getValue();

        assertThat(notification.getStatus()).isEqualTo(EmailNotification.EmailStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Should mark notification as failed when sending throws exception")
    void shouldMarkNotificationAsFailedWhenSendingThrowsException() {
        // Given
        SendEmailCommand command =
                new SendEmailCommand(
                        "test@example.com",
                        EmailTemplate.EMAIL_VERIFICATION,
                        Map.of("username", "John"));

        String errorMessage = "Connection timeout";
        doThrow(new EmailSendingException(errorMessage))
                .when(emailSender)
                .send(any(EmailNotification.class));

        // When & Then
        assertThatThrownBy(() -> notificationService.sendEmail(command))
                .isInstanceOf(EmailSendingException.class);

        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification notification = notificationCaptor.getValue();

        assertThat(notification.getStatus()).isEqualTo(EmailNotification.EmailStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("Should call email sender once when sending email")
    void shouldCallEmailSenderOnceWhenSendingEmail() {
        // Given
        SendEmailCommand command =
                new SendEmailCommand(
                        "test@example.com",
                        EmailTemplate.USER_REGISTERED,
                        Map.of("username", "John"));

        doNothing().when(emailSender).send(any(EmailNotification.class));

        // When
        notificationService.sendEmail(command);

        // Then
        verify(emailSender, times(1)).send(any(EmailNotification.class));
    }
}
