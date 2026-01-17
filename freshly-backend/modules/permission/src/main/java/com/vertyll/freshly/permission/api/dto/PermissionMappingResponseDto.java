package com.vertyll.freshly.permission.api.dto;

import java.util.UUID;

import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.permission.Permission;

public record PermissionMappingResponseDto(
        UUID id, UserRoleEnum keycloakRole, Permission permission) {}
