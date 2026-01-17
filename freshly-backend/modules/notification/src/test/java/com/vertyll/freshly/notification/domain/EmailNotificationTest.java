package com.vertyll.freshly.notification.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailNotificationTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String USERNAME_KEY = "username";
    private static final String USERNAME_VALUE = "John";
    private static final String LINK_KEY = "link";
    private static final String LINK_VALUE = "https://example.com";
    private static final String RESET_LINK_KEY = "resetLink";
    private static final String RESET_LINK_VALUE = "https://example.com/reset";
    private static final String NEW_KEY = "newKey";
    private static final String NEW_VALUE = "newValue";
    private static final String FAIL_KEY = "fail";
    private static final String FAIL_VALUE = "fail";

    private static final String SMTP_CONNECTION_FAILED = "SMTP connection failed";
    private static final String CONNECTION_TIMEOUT = "Connection timeout";

    private static final String RECIPIENT_CANNOT_BE_NULL = "Recipient cannot be null";
    private static final String TEMPLATE_CANNOT_BE_NULL = "Template cannot be null";
    private static final String TEMPLATE_VARIABLES_CANNOT_BE_NULL =
            "Template variables cannot be null";
    private static final String EMAIL_ALREADY_SENT = "Email already sent";

    @Test
    @DisplayName("Should create email notification with pending status")
    void shouldCreateEmailNotificationWithPendingStatus() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_VALUE, LINK_KEY, LINK_VALUE);

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
    @SuppressWarnings("NullAway")
    void shouldThrowExceptionWhenRecipientIsNull() {
        // Given
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_VALUE);

        // When & Then
        assertThatThrownBy(() -> new EmailNotification(null, template, variables))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(RECIPIENT_CANNOT_BE_NULL);
    }

    @Test
    @DisplayName("Should throw exception when template is null")
    @SuppressWarnings("NullAway")
    void shouldThrowExceptionWhenTemplateIsNull() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_VALUE);

        // When & Then
        assertThatThrownBy(() -> new EmailNotification(recipient, null, variables))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(TEMPLATE_CANNOT_BE_NULL);
    }

    @Test
    @DisplayName("Should throw exception when template variables are null")
    @SuppressWarnings("NullAway")
    void shouldThrowExceptionWhenTemplateVariablesAreNull() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;

        // When & Then
        assertThatThrownBy(() -> new EmailNotification(recipient, template, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(TEMPLATE_VARIABLES_CANNOT_BE_NULL);
    }

    @Test
    @DisplayName("Should mark notification as sent")
    void shouldMarkNotificationAsSent() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_VALUE);
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
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_VALUE);
        EmailNotification notification = new EmailNotification(recipient, template, variables);
        notification.markAsSent();

        // When & Then
        assertThatThrownBy(notification::markAsSent)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(EMAIL_ALREADY_SENT);
    }

    @Test
    @DisplayName("Should mark notification as failed with error message")
    void shouldMarkNotificationAsFailedWithErrorMessage() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_VALUE);
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        // When
        notification.markAsFailed(SMTP_CONNECTION_FAILED);

        // Then
        assertThat(notification.getStatus()).isEqualTo(EmailNotification.EmailStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo(SMTP_CONNECTION_FAILED);
    }

    @Test
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    @DisplayName("Should create immutable copy of template variables")
    void shouldCreateImmutableCopyOfTemplateVariables() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.EMAIL_VERIFICATION;

        Map<String, Object> variables = new java.util.HashMap<>();
        variables.put(USERNAME_KEY, USERNAME_VALUE);

        // When
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        variables.put(NEW_KEY, NEW_VALUE);

        Map<String, Object> notificationVariables = notification.getTemplateVariables();

        // Then
        assertThat(notification.getTemplateVariables()).doesNotContainKey(NEW_KEY);
        assertThat(notification.getTemplateVariables()).hasSize(1);

        assertThatThrownBy(() -> notificationVariables.put(FAIL_KEY, FAIL_VALUE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should reconstitute notification from database")
    @SuppressWarnings("NullAway")
    void shouldReconstituteNotificationFromDatabase() {
        // Given
        UUID id = UUID.randomUUID();
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.PASSWORD_RESET;
        Map<String, Object> variables =
                Map.of(USERNAME_KEY, USERNAME_VALUE, RESET_LINK_KEY, RESET_LINK_VALUE);
        LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC).minusHours(1);
        EmailNotification.EmailStatus status = EmailNotification.EmailStatus.SENT;
        LocalDateTime sentAt = LocalDateTime.now(ZoneOffset.UTC);

        // When
        EmailNotification notification =
                EmailNotification.reconstitute(
                        id, recipient, template, variables, createdAt, status, sentAt, null);

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
    @SuppressWarnings("NullAway")
    void shouldReconstituteFailedNotificationFromDatabase() {
        // Given
        UUID id = UUID.randomUUID();
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.USER_REGISTERED;
        Map<String, Object> variables = Map.of(USERNAME_KEY, USERNAME_VALUE);
        LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC).minusHours(1);
        EmailNotification.EmailStatus status = EmailNotification.EmailStatus.FAILED;

        // When
        EmailNotification notification =
                EmailNotification.reconstitute(
                        id,
                        recipient,
                        template,
                        variables,
                        createdAt,
                        status,
                        null,
                        CONNECTION_TIMEOUT);

        // Then
        assertThat(notification.getStatus()).isEqualTo(EmailNotification.EmailStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo(CONNECTION_TIMEOUT);
        assertThat(notification.getSentAt()).isNull();
    }

    @Test
    @DisplayName("Should handle empty template variables")
    void shouldHandleEmptyTemplateVariables() {
        // Given
        Email recipient = new Email(TEST_EMAIL);
        EmailTemplate template = EmailTemplate.USER_REGISTERED;
        Map<String, Object> variables = Map.of();

        // When
        EmailNotification notification = new EmailNotification(recipient, template, variables);

        // Then
        assertThat(notification.getTemplateVariables()).isEmpty();
        assertThat(notification.getStatus()).isEqualTo(EmailNotification.EmailStatus.PENDING);
    }
}
