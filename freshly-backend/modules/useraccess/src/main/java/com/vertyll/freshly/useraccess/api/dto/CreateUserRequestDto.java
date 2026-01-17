package com.vertyll.freshly.useraccess.api.dto;

import java.util.Set;
import java.util.UUID;

import com.vertyll.freshly.common.enums.UserRoleEnum;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequestDto(
        @NotNull(message = "{validation.user.keycloakId.required}") UUID keycloakUserId,
        boolean isActive,
        @NotNull(message = "{validation.user.roles.required}")
                @NotEmpty(message = "{validation.user.roles.required}")
                Set<UserRoleEnum> roles) {}
