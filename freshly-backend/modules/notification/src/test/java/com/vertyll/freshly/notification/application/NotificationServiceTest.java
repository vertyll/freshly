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

    private static final String TEST_EMAIL = "test@example.com";
    private static final String USER_EMAIL = "user@example.com";
    private static final String USERNAME_KEY = "username";
    private static final String USERNAME_VALUE_JOHN = "John";
    private static final String USERNAME_VALUE_JOHN_DOE = "John Doe";
    private static final String VERIFICATION_LINK_KEY = "verificationLink";
    private static final String VERIFICATION_LINK_VALUE = "https://example.com/verify/token123";
    private static final String RESET_LINK_KEY = "resetLink";
    private static final String RESET_LINK_VALUE = "https://example.com/reset/token456";

    private static final String SMTP_ERROR_MESSAGE = "SMTP error";
    private static final String CONNECTION_TIMEOUT_MESSAGE = "Connection timeout";

    private static final int EXPECTED_VARIABLE_SIZE_ONE = 1;
    private static final int EXPECTED_SENDER_INVOCATIONS = 1;

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
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables =
                Map.of(
                        USERNAME_KEY,
                        USERNAME_VALUE_JOHN,
                        VERIFICATION_LINK_KEY,
                        VERIFICATION_LINK_VALUE);
        SendEmailCommand command = new SendEmailCommand(TEST_EMAIL, template, variables);

        doNothing().when(emailSender).send(any(EmailNotification.class));

        // When
        notificationService.sendEmail(command);

        // Then
        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification sentNotification = notificationCaptor.getValue();

        assertThat(sentNotification.getRecipient().value()).isEqualTo(TEST_EMAIL);
        assertThat(sentNotification.getTemplate()).isEqualTo(template);
        assertThat(sentNotification.getTemplateVariables()).isEqualTo(variables);
        assertThat(sentNotification.getStatus()).isEqualTo(EmailNotification.EmailStatus.SENT);
    }

    @Test
    @DisplayName("Should throw exception when email sending fails")
    void shouldThrowExceptionWhenEmailSendingFails() {
        // Given
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_VALUE_JOHN);
        SendEmailCommand command = new SendEmailCommand(TEST_EMAIL, template, variables);

        EmailSendingException exception = new EmailSendingException(SMTP_ERROR_MESSAGE);
        doThrow(exception).when(emailSender).send(any(EmailNotification.class));

        // When & Then
        assertThatThrownBy(() -> notificationService.sendEmail(command))
                .isInstanceOf(EmailSendingException.class)
                .hasMessageContaining(SMTP_ERROR_MESSAGE);

        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification failedNotification = notificationCaptor.getValue();
        assertThat(failedNotification.getStatus()).isEqualTo(EmailNotification.EmailStatus.FAILED);
        assertThat(failedNotification.getErrorMessage()).isEqualTo(SMTP_ERROR_MESSAGE);
    }

    @Test
    @DisplayName("Should send email verification successfully")
    void shouldSendEmailVerificationSuccessfully() {
        // Given
        doNothing().when(emailSender).send(any(EmailNotification.class));

        // When
        notificationService.sendEmailVerification(
                USER_EMAIL, USERNAME_VALUE_JOHN_DOE, VERIFICATION_LINK_VALUE);

        // Then
        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification sentNotification = notificationCaptor.getValue();

        assertThat(sentNotification.getRecipient().value()).isEqualTo(USER_EMAIL);
        assertThat(sentNotification.getTemplate()).isEqualTo(EmailTemplate.EMAIL_VERIFICATION);
        assertThat(sentNotification.getTemplateVariables())
                .containsEntry(USERNAME_KEY, USERNAME_VALUE_JOHN_DOE);
        assertThat(sentNotification.getTemplateVariables())
                .containsEntry(VERIFICATION_LINK_KEY, VERIFICATION_LINK_VALUE);
    }

    @Test
    @DisplayName("Should send welcome email successfully")
    void shouldSendWelcomeEmailSuccessfully() {
        // Given
        doNothing().when(emailSender).send(any(EmailNotification.class));

        // When
        notificationService.sendWelcomeEmail(USER_EMAIL, USERNAME_VALUE_JOHN_DOE);

        // Then
        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification sentNotification = notificationCaptor.getValue();

        assertThat(sentNotification.getRecipient().value()).isEqualTo(USER_EMAIL);
        assertThat(sentNotification.getTemplate()).isEqualTo(EmailTemplate.USER_REGISTERED);
        assertThat(sentNotification.getTemplateVariables())
                .containsEntry(USERNAME_KEY, USERNAME_VALUE_JOHN_DOE);
        assertThat(sentNotification.getTemplateVariables()).hasSize(EXPECTED_VARIABLE_SIZE_ONE);
    }

    @Test
    @DisplayName("Should send password reset email successfully")
    void shouldSendPasswordResetEmailSuccessfully() {
        // Given
        doNothing().when(emailSender).send(any(EmailNotification.class));

        // When
        notificationService.sendPasswordResetEmail(
                USER_EMAIL, USERNAME_VALUE_JOHN_DOE, RESET_LINK_VALUE);

        // Then
        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification sentNotification = notificationCaptor.getValue();

        assertThat(sentNotification.getRecipient().value()).isEqualTo(USER_EMAIL);
        assertThat(sentNotification.getTemplate()).isEqualTo(EmailTemplate.PASSWORD_RESET);
        assertThat(sentNotification.getTemplateVariables())
                .containsEntry(USERNAME_KEY, USERNAME_VALUE_JOHN_DOE);
        assertThat(sentNotification.getTemplateVariables())
                .containsEntry(RESET_LINK_KEY, RESET_LINK_VALUE);
    }

    @Test
    @DisplayName("Should mark notification as sent after successful sending")
    void shouldMarkNotificationAsSentAfterSuccessfulSending() {
        // Given
        SendEmailCommand command =
                new SendEmailCommand(
                        TEST_EMAIL,
                        EmailTemplate.USER_REGISTERED,
                        Map.of(USERNAME_KEY, USERNAME_VALUE_JOHN));

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
                        TEST_EMAIL,
                        EmailTemplate.EMAIL_VERIFICATION,
                        Map.of(USERNAME_KEY, USERNAME_VALUE_JOHN));

        doThrow(new EmailSendingException(CONNECTION_TIMEOUT_MESSAGE))
                .when(emailSender)
                .send(any(EmailNotification.class));

        // When & Then
        assertThatThrownBy(() -> notificationService.sendEmail(command))
                .isInstanceOf(EmailSendingException.class);

        verify(emailSender).send(notificationCaptor.capture());
        EmailNotification notification = notificationCaptor.getValue();

        assertThat(notification.getStatus()).isEqualTo(EmailNotification.EmailStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo(CONNECTION_TIMEOUT_MESSAGE);
    }

    @Test
    @DisplayName("Should call email sender once when sending email")
    void shouldCallEmailSenderOnceWhenSendingEmail() {
        // Given
        SendEmailCommand command =
                new SendEmailCommand(
                        TEST_EMAIL,
                        EmailTemplate.USER_REGISTERED,
                        Map.of(USERNAME_KEY, USERNAME_VALUE_JOHN));

        doNothing().when(emailSender).send(any(EmailNotification.class));

        // When
        notificationService.sendEmail(command);

        // Then
        verify(emailSender, times(EXPECTED_SENDER_INVOCATIONS)).send(any(EmailNotification.class));
    }
}
