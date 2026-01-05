package com.vertyll.freshly.auth.domain.exception;

import java.io.Serial;

public class InvalidVerificationTokenException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;

    public InvalidVerificationTokenException(String message) {
        super(message);
    }

    public InvalidVerificationTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
