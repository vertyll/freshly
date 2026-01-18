package com.vertyll.freshly.permission.api.dto;

import java.util.UUID;

import com.vertyll.freshly.common.enums.Permission;

public record PermissionMappingResponseDto(UUID id, String keycloakRole, Permission permission) {}
