package com.vertyll.freshly.useraccess.domain.exception;

import java.io.Serial;
import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public UserNotFoundException(UUID userId) {
        super(String.format("User with ID %s not found.", userId));
    }
}
