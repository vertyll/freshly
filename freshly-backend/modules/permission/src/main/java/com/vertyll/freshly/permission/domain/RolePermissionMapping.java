package com.vertyll.freshly.permission.domain;

import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Getter;

import com.vertyll.freshly.common.enums.Permission;
import com.vertyll.freshly.common.enums.UserRoleEnum;

/** Domain entity representing the mapping between Keycloak roles and system permissions. */
@Getter
public final class RolePermissionMapping {
    private static final String ID_CANNOT_BE_NULL = "ID cannot be null";
    private static final String KEYCLOAK_ROLE_CANNOT_BE_NULL = "Keycloak role cannot be null";
    private static final String PERMISSION_CANNOT_BE_NULL = "Permission cannot be null";

    private final UUID id;
    private final UserRoleEnum keycloakRole;
    private final Permission permission;
    @Nullable private final Long version;

    public RolePermissionMapping(UserRoleEnum keycloakRole, Permission permission) {
        this(UUID.randomUUID(), keycloakRole, permission, null);
    }

    private RolePermissionMapping(
            UUID id, UserRoleEnum keycloakRole, Permission permission, @Nullable Long version) {
        this.id = Objects.requireNonNull(id, ID_CANNOT_BE_NULL);
        this.keycloakRole = Objects.requireNonNull(keycloakRole, KEYCLOAK_ROLE_CANNOT_BE_NULL);
        this.permission = Objects.requireNonNull(permission, PERMISSION_CANNOT_BE_NULL);
        this.version = version;
    }

    public static RolePermissionMapping reconstitute(
            UUID id, UserRoleEnum keycloakRole, Permission permission, Long version) {
        Objects.requireNonNull(id, ID_CANNOT_BE_NULL);
        Objects.requireNonNull(keycloakRole, KEYCLOAK_ROLE_CANNOT_BE_NULL);
        Objects.requireNonNull(permission, PERMISSION_CANNOT_BE_NULL);
        return new RolePermissionMapping(id, keycloakRole, permission, version);
    }
}
