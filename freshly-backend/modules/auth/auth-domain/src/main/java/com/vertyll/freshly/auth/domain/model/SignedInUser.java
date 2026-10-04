package com.vertyll.freshly.auth.domain.model;

import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record SignedInUser(UUID subject, String email, Set<String> roles) {

    public SignedInUser {
        requireNonNull(subject, "subject");
        requireNonNull(email, "email");
        roles = Set.copyOf(roles);
    }
}
