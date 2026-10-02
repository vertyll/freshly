package com.vertyll.freshly.auth.domain.error;

import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.error.ErrorKind;

public enum AuthError implements DomainError {
    SIGN_IN_REJECTED("error.auth.signInRejected", ErrorKind.UNAUTHENTICATED),
    SESSION_EXPIRED("error.auth.sessionExpired", ErrorKind.UNAUTHENTICATED),
    IDENTITY_PROVIDER_UNAVAILABLE("error.auth.identityProviderUnavailable", ErrorKind.EXTERNAL_SERVICE_FAILURE);

    private final String key;
    private final ErrorKind kind;

    AuthError(String key, ErrorKind kind) {
        this.key = key;
        this.kind = kind;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public ErrorKind kind() {
        return kind;
    }
}
