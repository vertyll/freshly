package com.vertyll.freshly.notification.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.notification.domain.error.NotificationError;

import static java.util.Objects.requireNonNull;

public final class EmailNotification {
    private static final String RECIPIENT_NULL = "Recipient cannot be null";
    private static final String TEMPLATE_NULL = "Template cannot be null";
    private static final String VARIABLES_NULL = "Template variables cannot be null";
    private static final String LANGUAGE_NULL = "Language cannot be null";

    private final UUID id;
    private final Email recipient;
    private final EmailTemplate template;
    private final Map<String, Object> templateVariables;

    private final String languageTag;

    private final Instant createdAt;

    private DeliveryStatus status;
    @Nullable private Instant sentAt;
    @Nullable private String errorMessage;

    @SuppressWarnings("java:S107")
    private EmailNotification(
        UUID id,
        Email recipient,
        EmailTemplate template,
        Map<String, Object> templateVariables,
        String languageTag,
        Instant createdAt,
        DeliveryStatus status,
        @Nullable Instant sentAt,
        @Nullable String errorMessage
    ) {
        this.id = requireNonNull(id);
        this.recipient = requireNonNull(recipient, RECIPIENT_NULL);
        this.template = requireNonNull(template, TEMPLATE_NULL);
        this.templateVariables = Map.copyOf(requireNonNull(templateVariables, VARIABLES_NULL));
        this.languageTag = requireNonNull(languageTag, LANGUAGE_NULL);
        this.createdAt = requireNonNull(createdAt);
        this.status = requireNonNull(status);
        this.sentAt = sentAt;
        this.errorMessage = errorMessage;
    }

    public static EmailNotification create(
        Email recipient,
        EmailTemplate template,
        Map<String, Object> templateVariables,
        String languageTag
    ) {
        return new EmailNotification(
            UUID.randomUUID(),
            recipient,
            template,
            templateVariables,
            languageTag,
            Instant.now(),
            DeliveryStatus.PENDING,
            null,
            null
        );
    }

    @SuppressWarnings("java:S107")
    public static EmailNotification reconstitute(
        UUID id,
        Email recipient,
        EmailTemplate template,
        Map<String, Object> templateVariables,
        String languageTag,
        Instant createdAt,
        DeliveryStatus status,
        @Nullable Instant sentAt,
        @Nullable String errorMessage
    ) {
        return new EmailNotification(
            id,
            recipient,
            template,
            templateVariables,
            languageTag,
            createdAt,
            status,
            sentAt,
            errorMessage
        );
    }

    public void markAsSent() {
        if (status == DeliveryStatus.SENT) {
            throw new DomainException(NotificationError.ALREADY_SENT, Map.of("notificationId", id));
        }
        status = DeliveryStatus.SENT;
        sentAt = Instant.now();
    }

    public void markAsFailed(@Nullable String reason) {
        if (status == DeliveryStatus.SENT) {
            throw new DomainException(NotificationError.ALREADY_SENT, Map.of("notificationId", id));
        }
        status = DeliveryStatus.FAILED;
        errorMessage = reason;
    }

    public UUID id() {
        return id;
    }

    public Email recipient() {
        return recipient;
    }

    public EmailTemplate template() {
        return template;
    }

    public Map<String, Object> templateVariables() {
        return templateVariables;
    }

    public String languageTag() {
        return languageTag;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public DeliveryStatus status() {
        return status;
    }

    @Nullable public Instant sentAt() {
        return sentAt;
    }

    @Nullable public String errorMessage() {
        return errorMessage;
    }
}
