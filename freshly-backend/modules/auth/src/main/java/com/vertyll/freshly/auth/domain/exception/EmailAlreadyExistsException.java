package com.vertyll.freshly.auth.domain.exception;

import java.io.Serial;

public class EmailAlreadyExistsException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;

    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email);
    }
}
