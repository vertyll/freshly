package com.vertyll.freshly.auth.infrastructure.web.dto;

import java.util.Set;
import java.util.UUID;

import com.vertyll.freshly.auth.domain.model.SignedInUser;

public record SessionResponseDto(UUID userId, String email, Set<String> roles) {
    public static SessionResponseDto from(SignedInUser user) {
        return new SessionResponseDto(user.subject(), user.email(), user.roles());
    }
}
