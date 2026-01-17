package com.vertyll.freshly.permission.api.dto;

import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.permission.Permission;

import jakarta.validation.constraints.NotNull;

public record CreatePermissionMappingDto(
        @NotNull(message = "{validation.permission.keycloakRole.required}")
                UserRoleEnum keycloakRole,
        @NotNull(message = "{validation.permission.permission.required}") Permission permission) {}
