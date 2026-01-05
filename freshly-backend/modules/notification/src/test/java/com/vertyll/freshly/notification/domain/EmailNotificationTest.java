package com.vertyll.freshly.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class EmailNotificationTest {

    @Test
    @DisplayName("Should create email notification with pending status")
    void shouldCreateEmailNotificationWithPendingStatus() {
        // Given
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of("username", "John", "link", "https://example.com");

        // When
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        // Then
        assertThat(notification.getId()).isNotNull();
        assertThat(notification.getRecipient()).isEqualTo(recipient);
        assertThat(notification.getTemplate()).isEqualTo(template);
        assertThat(notification.getTemplateVariables()).isEqualTo(variables);
        assertThat(notification.getStatus()).isEqualTo(EmailNotification.EmailStatus.PENDING);
        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getSentAt()).isNull();
        assertThat(notification.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should throw exception when recipient is null")
    void shouldThrowExceptionWhenRecipientIsNull() {
        // Given
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of("username", "John");

        // When & Then
        assertThatThrownBy(() -> new EmailNotification(null, template, variables))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Recipient cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when template is null")
    void shouldThrowExceptionWhenTemplateIsNull() {
        // Given
        Email recipient = new Email("test@example.com");
        Map<String, Object> variables = Map.of("username", "John");

        // When & Then
        assertThatThrownBy(() -> new EmailNotification(recipient, null, variables))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Template cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when template variables are null")
    void shouldThrowExceptionWhenTemplateVariablesAreNull() {
        // Given
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;

        // When & Then
        assertThatThrownBy(() -> new EmailNotification(recipient, template, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Template variables cannot be null");
    }

    @Test
    @DisplayName("Should mark notification as sent")
    void shouldMarkNotificationAsSent() {
        // Given
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of("username", "John");
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        // When
        notification.markAsSent();

        // Then
        assertThat(notification.getStatus()).isEqualTo(EmailNotification.EmailStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
        assertThat(notification.getSentAt()).isBeforeOrEqualTo(LocalDateTime.now(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Should throw exception when marking already sent notification as sent")
    void shouldThrowExceptionWhenMarkingAlreadySentNotificationAsSent() {
        // Given
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of("username", "John");
        EmailNotification notification = new EmailNotification(recipient, template, variables);
        notification.markAsSent();

        // When & Then
        assertThatThrownBy(notification::markAsSent)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Email already sent");
    }

    @Test
    @DisplayName("Should mark notification as failed with error message")
    void shouldMarkNotificationAsFailedWithErrorMessage() {
        // Given
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of("username", "John");
        EmailNotification notification = new EmailNotification(recipient, template, variables);
        String errorMessage = "SMTP connection failed";

        // When
        notification.markAsFailed(errorMessage);

        // Then
        assertThat(notification.getStatus()).isEqualTo(EmailNotification.EmailStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo(errorMessage);
    }

    @Test
    @SuppressWarnings("PMD.UseConcurrentHashMap") // HashMap is safe here (no concurrent access in test)
    @DisplayName("Should create immutable copy of template variables")
    void shouldCreateImmutableCopyOfTemplateVariables() {
        // Given
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        
        Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("username", "John");

        // When
        EmailNotification notification = new EmailNotification(recipient, template, variables);
        
        variables.put("newKey", "newValue");

        Map<String, Object> notificationVariables = notification.getTemplateVariables();

        // Then
        assertThat(notification.getTemplateVariables()).doesNotContainKey("newKey");
        assertThat(notification.getTemplateVariables()).hasSize(1);
        
        assertThatThrownBy(() -> notificationVariables.put("fail", "fail"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should reconstitute notification from database")
    void shouldReconstituteNotificationFromDatabase() {
        // Given
        UUID id = UUID.randomUUID();
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.PASSWORD_RESET;
        Map<String, Object> variables = Map.of("username", "John", "resetLink", "https://example.com/reset");
        LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC).minusHours(1);
        EmailNotification.EmailStatus status = EmailNotification.EmailStatus.SENT;
        LocalDateTime sentAt = LocalDateTime.now(ZoneOffset.UTC);

        // When
        EmailNotification notification = EmailNotification.reconstitute(
                id, recipient, template, variables, createdAt, status, sentAt, null
        );

        // Then
        assertThat(notification.getId()).isEqualTo(id);
        assertThat(notification.getRecipient()).isEqualTo(recipient);
        assertThat(notification.getTemplate()).isEqualTo(template);
        assertThat(notification.getTemplateVariables()).isEqualTo(variables);
        assertThat(notification.getCreatedAt()).isEqualTo(createdAt);
        assertThat(notification.getStatus()).isEqualTo(status);
        assertThat(notification.getSentAt()).isEqualTo(sentAt);
        assertThat(notification.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Should reconstitute failed notification from database")
    void shouldReconstituteFailedNotificationFromDatabase() {
        // Given
        UUID id = UUID.randomUUID();
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.USER_REGISTERED;
        Map<String, Object> variables = Map.of("username", "John");
        LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC).minusHours(1);
        EmailNotification.EmailStatus status = EmailNotification.EmailStatus.FAILED;
        String errorMessage = "Connection timeout";

        // When
        EmailNotification notification = EmailNotification.reconstitute(
                id, recipient, template, variables, createdAt, status, null, errorMessage
        );

        // Then
        assertThat(notification.getStatus()).isEqualTo(EmailNotification.EmailStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(notification.getSentAt()).isNull();
    }

    @Test
    @DisplayName("Should handle empty template variables")
    void shouldHandleEmptyTemplateVariables() {
        // Given
        Email recipient = new Email("test@example.com");
        EmailTemplate template = EmailTemplate.USER_REGISTERED;
        Map<String, Object> variables = Map.of();

        // When
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        // Then
        assertThat(notification.getTemplateVariables()).isEmpty();
        assertThat(notification.getStatus()).isEqualTo(EmailNotification.EmailStatus.PENDING);
    }
}
