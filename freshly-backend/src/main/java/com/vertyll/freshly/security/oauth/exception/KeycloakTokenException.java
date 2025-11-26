package com.vertyll.freshly.security.oauth.exception;

public class KeycloakTokenException extends RuntimeException {
    public KeycloakTokenException(String message) {
        super(message);
    }

    public KeycloakTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
