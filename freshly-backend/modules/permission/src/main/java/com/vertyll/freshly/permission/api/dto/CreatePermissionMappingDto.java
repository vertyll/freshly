package com.vertyll.freshly.permission.api.dto;

import com.vertyll.freshly.common.enums.Permission;
import com.vertyll.freshly.common.enums.UserRoleEnum;

import jakarta.validation.constraints.NotNull;

public record CreatePermissionMappingDto(
        @NotNull(message = "{validation.permission.keycloakRole.required}")
                UserRoleEnum keycloakRole,
        @NotNull(message = "{validation.permission.permission.required}") Permission permission) {}
