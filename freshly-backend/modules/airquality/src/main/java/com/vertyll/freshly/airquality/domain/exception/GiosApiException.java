package com.vertyll.freshly.airquality.domain.exception;

import java.io.Serial;

public class GiosApiException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public GiosApiException(String message) {
        super(message);
    }

    public GiosApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
