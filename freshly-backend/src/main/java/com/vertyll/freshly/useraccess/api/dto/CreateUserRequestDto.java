package com.vertyll.freshly.useraccess.api.dto;

import com.vertyll.freshly.useraccess.domain.UserRoleEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record CreateUserRequestDto(
        @NotNull(message = "{validation.user.keycloakId.required}")
        UUID keycloakUserId,

        boolean isActive,

        @NotNull(message = "{validation.user.roles.required}")
        @NotEmpty(message = "{validation.user.roles.required}")
        Set<UserRoleEnum> roles
) {
}
