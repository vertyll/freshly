package com.vertyll.freshly.useraccess.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.useraccess.application.dto.UserResponse;

public record UserResponseDto(UUID keycloakUserId, boolean isActive, List<String> roles, @Nullable Long version) {
    public static UserResponseDto from(UserResponse response) {
        return new UserResponseDto(response.keycloakUserId(), response.active(), response.roles(), response.version());
    }
}
