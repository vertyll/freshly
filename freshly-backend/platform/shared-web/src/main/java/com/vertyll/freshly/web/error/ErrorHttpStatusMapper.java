package com.vertyll.freshly.web.error;

import org.springframework.http.HttpStatus;

import com.vertyll.freshly.lang.error.ErrorKind;

/**
 * Turns a failure's kind into a status, in exactly one place.
 */
public final class ErrorHttpStatusMapper {

    private ErrorHttpStatusMapper() {
    }

    public static HttpStatus statusOf(ErrorKind kind) {
        return switch (kind) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INVALID -> HttpStatus.BAD_REQUEST;
            case PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED;
            case GONE -> HttpStatus.GONE;
            // A misconfiguration is the application's fault, not the caller's, so it
            // must not come back as a 4xx: a client that retries after "fixing" its
            // request would loop forever against a problem only an operator can clear.
            case MISCONFIGURED -> HttpStatus.INTERNAL_SERVER_ERROR;
            case EXTERNAL_SERVICE_FAILURE -> HttpStatus.BAD_GATEWAY;
        };
    }
}
