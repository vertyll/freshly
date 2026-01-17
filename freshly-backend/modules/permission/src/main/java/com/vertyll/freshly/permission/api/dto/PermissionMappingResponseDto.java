package com.vertyll.freshly.permission.api.dto;

import java.util.UUID;

import com.vertyll.freshly.common.enums.Permission;
import com.vertyll.freshly.common.enums.UserRoleEnum;

public record PermissionMappingResponseDto(
        UUID id, UserRoleEnum keycloakRole, Permission permission) {}
