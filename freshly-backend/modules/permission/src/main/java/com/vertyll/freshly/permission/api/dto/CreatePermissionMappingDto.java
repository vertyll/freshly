package com.vertyll.freshly.permission.api.dto;

import com.vertyll.freshly.common.enums.Permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePermissionMappingDto(
        @NotBlank(message = "{validation.permission.keycloakRole.required}") String keycloakRole,
        @NotNull(message = "{validation.permission.permission.required}") Permission permission) {}
