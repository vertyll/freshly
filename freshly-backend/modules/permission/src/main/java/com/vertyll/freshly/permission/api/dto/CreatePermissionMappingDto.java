package com.vertyll.freshly.permission.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionMappingDto(
        @NotBlank(message = "{validation.permission.keycloakRole.required}") String keycloakRole,
        @NotBlank(message = "{validation.permission.permission.required}") String permission) {}
