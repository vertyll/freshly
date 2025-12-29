package com.vertyll.freshly.permission.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionMappingDto(
        @NotBlank(message = "Keycloak role cannot be blank")
        String keycloakRole,

        @NotBlank(message = "Permission cannot be blank")
        String permission
) {
}
