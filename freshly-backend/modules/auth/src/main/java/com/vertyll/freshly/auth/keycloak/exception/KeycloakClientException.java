package com.vertyll.freshly.auth.keycloak.exception;

import java.io.Serial;

public class KeycloakClientException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;

    public KeycloakClientException(String message) {
        super(message);
    }

    public KeycloakClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
