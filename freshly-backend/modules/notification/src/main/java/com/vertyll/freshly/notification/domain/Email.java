package com.vertyll.freshly.notification.domain;

import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final String EMAIL_NULL = "Email cannot be null";
    private static final String INVALID_EMAIL_FORMAT = "Invalid email format: ";

    public Email {
        Objects.requireNonNull(value, EMAIL_NULL);
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(INVALID_EMAIL_FORMAT + value);
        }
    }

    @Override
    public @NonNull String toString() {
        return value;
    }
}
