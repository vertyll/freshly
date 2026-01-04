package com.vertyll.freshly.security.keycloak;

import java.io.Serial;

/**
 * Exception thrown when Keycloak client operations fail
 */
public class KeycloakClientException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    
    public KeycloakClientException(String message) {
        super(message);
    }
    
    public KeycloakClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
