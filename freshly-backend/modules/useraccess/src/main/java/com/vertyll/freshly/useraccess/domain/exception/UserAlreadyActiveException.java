package com.vertyll.freshly.useraccess.domain.exception;

import java.io.Serial;
import java.util.UUID;

public final class UserAlreadyActiveException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public UserAlreadyActiveException() {
        super("User is already active.");
    }

    public UserAlreadyActiveException(UUID userId) {
        super(String.format("User with ID %s is already active.", userId));
    }
}
