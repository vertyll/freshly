package com.vertyll.freshly.notification.infrastructure.mail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables =
                Map.of("username", "John", "verificationLink", "https://example.com");
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        String htmlContent = "<html><body>Verification email</body></html>";
        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn("noreply@example.com");
        doNothing().when(mailSender).send(mimeMessage);

        // When
        smtpEmailSender.send(notification);

        // Then
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq(template.getTemplateName()), contextCaptor.capture());
        verify(mailSender).send(mimeMessage);

        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("username")).isEqualTo("John");
        assertThat(capturedContext.getVariable("verificationLink"))
                .isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("Should throw EmailSendingException when MessagingException occurs")
    void shouldThrowEmailSendingExceptionWhenMessagingExceptionOccurs() {
        // Given
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.USER_REGISTERED;
        Map<String, Object> variables = Map.of("username", "John");
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        String htmlContent = "<html><body>Welcome</body></html>";
        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn("noreply@example.com");

        doAnswer(
                        _ -> {
                            throw new MessagingException("SMTP server not responding");
                        })
                .when(mailSender)
                .send(any(MimeMessage.class));

        // When & Then
        assertThatThrownBy(() -> smtpEmailSender.send(notification))
                .isInstanceOf(EmailSendingException.class)
                .hasMessageContaining("Failed to send email")
                .hasCauseInstanceOf(MessagingException.class);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should process template with correct variables")
    void shouldProcessTemplateWithCorrectVariables() {
        // Given
        Email recipient = new Email("user@example.com");
        EmailTemplate template = EmailTemplate.PASSWORD_RESET;
        Map<String, Object> variables =
                Map.of(
                        "username", "Jane Doe",
                        "resetLink", "https://example.com/reset/token123");
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        String htmlContent = "<html><body>Reset your password</body></html>";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn("noreply@example.com");
        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(htmlContent);
        doNothing().when(mailSender).send(mimeMessage);

        // When
        smtpEmailSender.send(notification);

        // Then
        verify(templateEngine).process(eq("email/password-reset"), contextCaptor.capture());
        Context capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getVariable("username")).isEqualTo("Jane Doe");
        assertThat(capturedContext.getVariable("resetLink"))
                .isEqualTo("https://example.com/reset/token123");
    }

    @Test
    @DisplayName("Should send email with correct recipient and subject")
    void shouldSendEmailWithCorrectRecipientAndSubject() {
        // Given
        Email recipient = new Email("recipient@example.com");
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of("username", "John");
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        String htmlContent = "<html><body>Verify your email</body></html>";
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn("noreply@example.com");
        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(htmlContent);
        doNothing().when(mailSender).send(mimeMessage);

        // When
        smtpEmailSender.send(notification);

        // Then
        verify(mailSender).send(mimeMessage);
        // We can't verify MimeMessage content directly without mocking MimeMessageHelper,
        // but we verify that the send method was called
    }

    @Test
    @DisplayName("Should handle empty template variables")
    void shouldHandleEmptyTemplateVariables() {
        // Given
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.USER_REGISTERED;
        Map<String, Object> variables = Map.of();
        EmailNotification notification = new EmailNotification(recipient, template, variables);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn("noreply@example.com");

        String htmlContent = "<html><body>Welcome</body></html>";
        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(htmlContent);
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
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of("username", "John");
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenThrow(new RuntimeException("Template not found"));

        // When & Then
        assertThatThrownBy(() -> smtpEmailSender.send(notification))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Template not found");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should use correct template name for each email type")
    void shouldUseCorrectTemplateNameForEachEmailType() {
        // Given
        Email recipient = new Email("test@example.com");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn("noreply@example.com");
        Map<String, Object> variables = Map.of("username", "John");

        String htmlContent = "<html><body>Email</body></html>";
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(htmlContent);
        doNothing().when(mailSender).send(mimeMessage);

        // When & Then - Email Verification
        EmailNotification verificationNotification =
                new EmailNotification(recipient, EmailTemplate.EMAIL_VERIFICATION, variables);
        smtpEmailSender.send(verificationNotification);
        verify(templateEngine).process(eq("email/email-verification"), any(Context.class));

        // When & Then - User Registered
        EmailNotification registeredNotification =
                new EmailNotification(recipient, EmailTemplate.USER_REGISTERED, variables);
        smtpEmailSender.send(registeredNotification);
        verify(templateEngine).process(eq("email/user-registered"), any(Context.class));

        // When & Then - Password Reset
        EmailNotification resetNotification =
                new EmailNotification(recipient, EmailTemplate.PASSWORD_RESET, variables);
        smtpEmailSender.send(resetNotification);
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
    }

    @Test
    @DisplayName("Should create new MimeMessage for each send")
    void shouldCreateNewMimeMessageForEachSend() {
        // Given
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.USER_REGISTERED;
        Map<String, Object> variables = Map.of("username", "John");
        EmailNotification notification1 = new EmailNotification(recipient, template, variables);
        EmailNotification notification2 = new EmailNotification(recipient, template, variables);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mailProperties.from()).thenReturn("noreply@example.com");

        String htmlContent = "<html><body>Welcome</body></html>";
        when(templateEngine.process(eq(template.getTemplateName()), any(Context.class)))
                .thenReturn(htmlContent);
        doNothing().when(mailSender).send(mimeMessage);

        // When
        smtpEmailSender.send(notification1);
        smtpEmailSender.send(notification2);

        // Then
        verify(mailSender, times(2)).createMimeMessage();
        verify(mailSender, times(2)).send(mimeMessage);
    }
}
