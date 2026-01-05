package com.vertyll.freshly.notification.domain.exception;

import java.io.Serial;

public class EmailSendingException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;

    public EmailSendingException(String message) {
        super(message);
    }

    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
