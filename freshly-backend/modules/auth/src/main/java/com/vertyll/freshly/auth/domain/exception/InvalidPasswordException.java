package com.vertyll.freshly.auth.domain.exception;

import java.io.Serial;

public class InvalidPasswordException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;

    public InvalidPasswordException(String message) {
        super(message);
    }
}
