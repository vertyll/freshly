package com.vertyll.freshly.notification.domain.error;

import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.error.ErrorKind;

public enum NotificationError implements DomainError {
    DELIVERY_FAILED("error.notification.deliveryFailed", ErrorKind.EXTERNAL_SERVICE_FAILURE),
    TEMPLATE_RENDER_FAILED("error.notification.templateRenderFailed", ErrorKind.MISCONFIGURED),
    INVALID_RECIPIENT("error.notification.invalidRecipient", ErrorKind.INVALID),

    ALREADY_SENT("error.notification.alreadySent", ErrorKind.CONFLICT);

    private final String key;
    private final ErrorKind kind;

    NotificationError(String key, ErrorKind kind) {
        this.key = key;
        this.kind = kind;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public ErrorKind kind() {
        return kind;
    }
}
