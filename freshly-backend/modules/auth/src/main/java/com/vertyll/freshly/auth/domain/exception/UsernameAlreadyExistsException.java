package com.vertyll.freshly.auth.domain.exception;

import java.io.Serial;

public class UsernameAlreadyExistsException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public UsernameAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}
