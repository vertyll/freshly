package com.vertyll.freshly.useraccess.application.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.useraccess.domain.model.SystemUser;

public record UserResponse(UUID keycloakUserId, boolean active, List<String> roles, @Nullable Long version) {
    public UserResponse {
        roles = List.copyOf(roles);
    }

    public static UserResponse from(SystemUser user) {
        Set<String> roles = user.roles();
        return new UserResponse(
            user.keycloakUserId(),
            user.isActive(),
            roles.stream().sorted().toList(),
            user.version()
        );
    }
}
