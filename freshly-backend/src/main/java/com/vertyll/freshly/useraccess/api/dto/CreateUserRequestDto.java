package com.vertyll.freshly.useraccess.api.dto;

import com.vertyll.freshly.useraccess.domain.UserRoleEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequestDto(
        @NotNull(message = "Keycloak user ID is required")
        UUID keycloakUserId,

        boolean isActive,

        @NotNull(message = "Roles cannot be null")
        @NotEmpty(message = "Roles cannot be empty")
        Set<UserRoleEnum> roles
) {
}
