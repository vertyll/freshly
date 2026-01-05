package com.vertyll.freshly.permission.api.dto;

import java.util.UUID;

public record PermissionMappingResponseDto(
        UUID id,
        String keycloakRole,
        String permission
) {
}
