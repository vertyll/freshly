package com.vertyll.freshly.auth.domain.model;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record VerificationToken(UUID subject, String email, TokenPurpose purpose) {
    private static final String SUBJECT_NULL = "Token subject cannot be null";
    private static final String EMAIL_NULL = "Token email cannot be null";
    private static final String PURPOSE_NULL = "Token purpose cannot be null";

    public VerificationToken {
        requireNonNull(subject, SUBJECT_NULL);
        requireNonNull(email, EMAIL_NULL);
        requireNonNull(purpose, PURPOSE_NULL);
    }
}
