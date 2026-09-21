package com.vertyll.freshly.auth.domain.error;

import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.error.ErrorKind;

public enum AuthError implements DomainError {
    USERNAME_ALREADY_EXISTS("error.auth.usernameAlreadyExists", ErrorKind.CONFLICT),
    EMAIL_ALREADY_EXISTS("error.auth.emailAlreadyExists", ErrorKind.CONFLICT),

    INVALID_CREDENTIALS("error.auth.invalidCredentials", ErrorKind.UNAUTHENTICATED),
    CURRENT_PASSWORD_INCORRECT("error.auth.currentPasswordIncorrect", ErrorKind.UNAUTHENTICATED),

    TOKEN_INVALID("error.auth.tokenInvalid", ErrorKind.UNAUTHENTICATED),
    TOKEN_EXPIRED("error.auth.tokenExpired", ErrorKind.GONE),
    TOKEN_WRONG_PURPOSE("error.auth.tokenTypeInvalid", ErrorKind.UNAUTHENTICATED),
    TOKEN_MALFORMED("error.auth.tokenFormatInvalid", ErrorKind.UNAUTHENTICATED),

    REFRESH_TOKEN_INVALID("error.auth.refreshTokenInvalid", ErrorKind.UNAUTHENTICATED),

    USER_NOT_FOUND("error.auth.userNotFound", ErrorKind.NOT_FOUND),
    WEAK_PASSWORD("error.auth.weakPassword", ErrorKind.INVALID),

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
