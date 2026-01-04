package com.vertyll.freshly.notification.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
public class EmailNotification {

    private final UUID id;
    private final Email recipient;
    private final EmailTemplate template;
    private final Map<String, Object> templateVariables;
    private final LocalDateTime createdAt;
    private EmailStatus status;
    private LocalDateTime sentAt;
    private String errorMessage;

    public EmailNotification(
            Email recipient,
            EmailTemplate template,
            Map<String, Object> templateVariables
    ) {
        this.id = UUID.randomUUID();
        this.recipient = Objects.requireNonNull(recipient, "Recipient cannot be null");
        this.template = Objects.requireNonNull(template, "Template cannot be null");
        this.templateVariables = Map.copyOf(Objects.requireNonNull(templateVariables, "Template variables cannot be null"));
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        this.status = EmailStatus.PENDING;
    }

    @SuppressWarnings("java:S107")
    public static EmailNotification reconstitute(
            UUID id,
            Email recipient,
            EmailTemplate template,
            Map<String, Object> templateVariables,
            LocalDateTime createdAt,
            EmailStatus status,
            LocalDateTime sentAt,
            String errorMessage
    ) {
        return EmailNotification.builder()
                .id(id)
                .recipient(recipient)
                .template(template)
                .templateVariables(templateVariables)
                .createdAt(createdAt)
                .status(status)
                .sentAt(sentAt)
                .errorMessage(errorMessage)
                .build();
    }

    @Builder(access = AccessLevel.PRIVATE)
    @SuppressWarnings("java:S107")
    private EmailNotification(
            UUID id,
            Email recipient,
            EmailTemplate template,
            Map<String, Object> templateVariables,
            LocalDateTime createdAt,
            EmailStatus status,
            LocalDateTime sentAt,
            String errorMessage
    ) {
        this.id = id;
        this.recipient = recipient;
        this.template = template;
        this.templateVariables = templateVariables;
        this.createdAt = createdAt;
        this.status = status;
        this.sentAt = sentAt;
        this.errorMessage = errorMessage;
    }

    public void markAsSent() {
        if (this.status == EmailStatus.SENT) {
            throw new IllegalStateException("Email already sent");
        }
        this.status = EmailStatus.SENT;
        this.sentAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void markAsFailed(String errorMessage) {
        this.status = EmailStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public enum EmailStatus {
        PENDING,
        SENT,
        FAILED
    }
}