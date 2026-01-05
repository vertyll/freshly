package com.vertyll.freshly.useraccess.domain.exception;

import java.io.Serial;
import java.util.UUID;

public class UserAlreadyExistsException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;

    public UserAlreadyExistsException(UUID keycloakUserId) {
        super(String.format("User with ID %s already exists.", keycloakUserId));
    }
}
