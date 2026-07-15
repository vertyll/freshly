package com.vertyll.freshly.permission.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.vertyll.freshly.common.enums.Permission;

public record CreatePermissionMappingDto(
    @NotBlank(message = "{validation.permission.keycloakRole.required}") String keycloakRole,
    @NotNull(message = "{validation.permission.permission.required}") Permission permission
) {
}
