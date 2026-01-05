package com.vertyll.freshly.auth.domain.event;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String username,
        String email,
        LocalDateTime occurredAt
) {
    public UserRegisteredEvent(UUID userId, String username, String email) {
        this(userId, username, email, LocalDateTime.now(ZoneOffset.UTC));
    }
}
