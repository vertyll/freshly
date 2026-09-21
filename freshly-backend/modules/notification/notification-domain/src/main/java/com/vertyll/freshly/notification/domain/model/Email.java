package com.vertyll.freshly.notification.domain.model;

import java.util.Map;
import java.util.regex.Pattern;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.notification.domain.error.NotificationError;

import static java.util.Objects.requireNonNull;

public record Email(String value) {
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final String EMAIL_NULL = "Email cannot be null";
    private static final String MASK = "***";

    public Email {
        requireNonNull(value, EMAIL_NULL);
        if (!PATTERN.matcher(value).matches()) {
            throw new DomainException(NotificationError.INVALID_RECIPIENT, Map.of("email", value));
        }
    }

    @Override
    public String toString() {
        int at = value.indexOf('@');
        return at <= 1 ? MASK + value.substring(Math.max(at, 0)) : value.charAt(0) + MASK + value.substring(at);
    }
}
