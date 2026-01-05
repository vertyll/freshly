package com.vertyll.freshly.useraccess.domain.exception;

import java.io.Serial;

public class UserRolesEmptyException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public UserRolesEmptyException() {
        super("User must have at least one role");
    }
}
