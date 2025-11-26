package com.vertyll.freshly.useraccess.domain.exception;

public class UserRolesEmptyException extends RuntimeException {
    public UserRolesEmptyException() {
        super("User must have at least one role");
    }
}
