package com.vertyll.freshly.notification.infrastructure.mail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Map;

import com.vertyll.freshly.common.config.MailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.vertyll.freshly.notification.domain.Email;
import com.vertyll.freshly.notification.domain.EmailNotification;
import com.vertyll.freshly.notification.domain.EmailTemplate;
import com.vertyll.freshly.notification.domain.exception.EmailSendingException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSenderTest {

    private static final String USERNAME_KEY = "username";
    private static final String VERIFICATION_LINK_KEY = "verificationLink";
    private static final String RESET_LINK_KEY = "resetLink";

    private static final String TEST_EMAIL = "test@example.com";
    private static final String USER_EMAIL = "user@example.com";
    private static final String RECIPIENT_EMAIL = "recipient@example.com";
    private static final String NOREPLY_EMAIL = "noreply@example.com";

    private static final String USERNAME_JOHN = "John";
    private static final String USERNAME_JANE_DOE = "Jane Doe";

    private static final String VERIFICATION_LINK = "https://example.com";
    private static final String RESET_LINK = "https://example.com/reset/token123";

    private static final String HTML_VERIFICATION_EMAIL =
            "<html><body>Verification email</body></html>";
    private static final String HTML_WELCOME = "<html><body>Welcome</body></html>";
    private static final String HTML_PASSWORD_RESET =
            "<html><body>Reset your password</body></html>";
    private static final String HTML_VERIFY_EMAIL = "<html><body>Verify your email</body></html>";
    private static final String HTML_EMAIL = "<html><body>Email</body></html>";

    private static final String TEMPLATE_NOT_FOUND = "Template not found";
    private static final String SMTP_SERVER_NOT_RESPONDING = "SMTP server not responding";
    private static final String FAILED_TO_SEND_EMAIL = "Failed to send email";

    private static final String TEMPLATE_EMAIL_VERIFICATION = "email/email-verification";
    private static final String TEMPLATE_USER_REGISTERED = "email/user-registered";
    private static final String TEMPLATE_PASSWORD_RESET = "email/password-reset";

    private static final int EXPECTED_SEND_CALLS = 2;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private JavaMailSender mailSender;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private TemplateEngine templateEngine;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private MailProperties mailProperties;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private MimeMessage mimeMessage;

    @Captor
    @SuppressWarnings("NullAway.Init")
    private ArgumentCaptor<Context> contextCaptor;

    private SmtpEmailSender smtpEmailSender;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        smtpEmailSender = new SmtpEmailSender(mailSender, templateEngine, mailProperties);
    }

    @Test
    @DisplayName("Should send email successfully")
    void shouldSendEmailSuccessfully() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables =
                Map.of(USERNAME_KEY, USERNAME_JOHN, VERIFICATION_LINK_KEY, VERIFICATION_LINK);
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(HTML_VERIFICATION_EMAIL);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn(NOREPLY_EMAIL);
        doNothing().when(mailSender).send(mimeMessage);

        // When
        smtpEmailSender.send(notification);

        // Then
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq(template.getTemplateName()), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable(USERNAME_KEY)).isEqualTo(USERNAME_JOHN);
        assertThat(capturedContext.getVariable(VERIFICATION_LINK_KEY)).isEqualTo(VERIFICATION_LINK);
    }

    @Test
    @DisplayName("Should throw EmailSendingException when MessagingException occurs")
    void shouldThrowEmailSendingExceptionWhenMessagingExceptionOccurs() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.USER_REGISTERED;
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_JOHN);
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(HTML_WELCOME);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn(NOREPLY_EMAIL);

        doAnswer(
                        _ -> {
                            throw new MessagingException(SMTP_SERVER_NOT_RESPONDING);
                        })
                .when(mailSender)
                .send(any(MimeMessage.class));

        // When & Then
        assertThatThrownBy(() -> smtpEmailSender.send(notification))
                .isInstanceOf(EmailSendingException.class)
                .hasMessageContaining(FAILED_TO_SEND_EMAIL)
                .hasCauseInstanceOf(MessagingException.class);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should process template with correct variables")
    void shouldProcessTemplateWithCorrectVariables() {
        // Given
        Email recipient = new Email(USER_EMAIL);
        EmailTemplate template = EmailTemplate.PASSWORD_RESET;
        Map<String, Object> variables =
                Map.of(
                        USERNAME_KEY, USERNAME_JANE_DOE,
                        RESET_LINK_KEY, RESET_LINK);
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn(NOREPLY_EMAIL);
        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(HTML_PASSWORD_RESET);
        doNothing().when(mailSender).send(mimeMessage);

        // When
        smtpEmailSender.send(notification);

        // Then
        verify(templateEngine).process(eq(TEMPLATE_PASSWORD_RESET), contextCaptor.capture());
        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable(USERNAME_KEY)).isEqualTo(USERNAME_JANE_DOE);
        assertThat(capturedContext.getVariable(RESET_LINK_KEY)).isEqualTo(RESET_LINK);
    }

    @Test
    @DisplayName("Should send email with correct recipient and subject")
    void shouldSendEmailWithCorrectRecipientAndSubject() {
        // Given
        Email recipient = new Email(RECIPIENT_EMAIL);
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_JOHN);
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn(NOREPLY_EMAIL);
        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(HTML_VERIFY_EMAIL);
        doNothing().when(mailSender).send(mimeMessage);

        // When
        smtpEmailSender.send(notification);

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should handle empty template variables")
    void shouldHandleEmptyTemplateVariables() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.USER_REGISTERED;
        Map<String, Object> variables = Map.of();
        EmailNotification notification = new EmailNotification(recipient, template, variables);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn(NOREPLY_EMAIL);

        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(HTML_WELCOME);
        doNothing().when(mailSender).send(mimeMessage);

        // When
        smtpEmailSender.send(notification);

        // Then
        verify(templateEngine).process(eq(template.getTemplateName()), contextCaptor.capture());
        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariableNames()).isEmpty();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should throw exception when template processing fails")
    void shouldThrowExceptionWhenTemplateProcessingFails() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_JOHN);
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenThrow(new RuntimeException(TEMPLATE_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> smtpEmailSender.send(notification))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(TEMPLATE_NOT_FOUND);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should use correct template name for each email type")
    void shouldUseCorrectTemplateNameForEachEmailType() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn(NOREPLY_EMAIL);
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_JOHN);

        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_EMAIL);
        doNothing().when(mailSender).send(mimeMessage);

        // When & Then - Email Verification
        EmailNotification verificationNotification =
                new EmailNotification(recipient, EmailTemplate.EMAIL_VERIFICATION, variables);
        smtpEmailSender.send(verificationNotification);
        verify(templateEngine).process(eq(TEMPLATE_EMAIL_VERIFICATION), any(Context.class));

        // When & Then - User Registered
        EmailNotification registeredNotification =
                new EmailNotification(recipient, EmailTemplate.USER_REGISTERED, variables);
        smtpEmailSender.send(registeredNotification);
        verify(templateEngine).process(eq(TEMPLATE_USER_REGISTERED), any(Context.class));

        // When & Then - Password Reset
        EmailNotification resetNotification =
                new EmailNotification(recipient, EmailTemplate.PASSWORD_RESET, variables);
        smtpEmailSender.send(resetNotification);
        verify(templateEngine).process(eq(TEMPLATE_PASSWORD_RESET), any(Context.class));
    }

    @Test
    @DisplayName("Should create new MimeMessage for each send")
    void shouldCreateNewMimeMessageForEachSend() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.USER_REGISTERED;
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_JOHN);
        EmailNotification notification1 = new EmailNotification(recipient, template, variables);
        EmailNotification notification2 = new EmailNotification(recipient, template, variables);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn(NOREPLY_EMAIL);

        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(HTML_WELCOME);
        doNothing().when(mailSender).send(mimeMessage);

        // When
        smtpEmailSender.send(notification1);
        smtpEmailSender.send(notification2);

        // Then
        verify(mailSender, times(EXPECTED_SEND_CALLS)).createMimeMessage();
        verify(mailSender, times(EXPECTED_SEND_CALLS)).send(mimeMessage);
    }
}
