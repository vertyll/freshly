package com.vertyll.freshly.security.keycloak;

/**
 * Exception thrown when Keycloak client operations fail
 */
public class KeycloakClientException extends RuntimeException {
    
    public KeycloakClientException(String message) {
        super(message);
    }
    
    public KeycloakClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
