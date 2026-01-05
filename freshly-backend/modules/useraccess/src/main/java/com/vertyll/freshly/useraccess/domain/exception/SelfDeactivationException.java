package com.vertyll.freshly.useraccess.domain.exception;

import java.io.Serial;
import java.util.UUID;

public final class SelfDeactivationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SelfDeactivationException(UUID userId) {
        super(String.format("User with ID %s cannot deactivate themselves.", userId));
    }
}
