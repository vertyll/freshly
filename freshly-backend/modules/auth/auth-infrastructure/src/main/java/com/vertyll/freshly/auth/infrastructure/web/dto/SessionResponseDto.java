package com.vertyll.freshly.auth.infrastructure.web.dto;

import java.util.Set;
import java.util.UUID;

import com.vertyll.freshly.auth.domain.model.AuthSession;

public record SessionResponseDto(UUID userId, String email, Set<String> roles) {
    public static SessionResponseDto from(AuthSession session) {
        return new SessionResponseDto(session.subject(), session.email(), session.roles());
    }
}
