package com.vertyll.freshly.permission.domain;

import com.vertyll.freshly.permission.Permission;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing the mapping between Keycloak roles and system permissions.
 */
@Getter
@AllArgsConstructor
public final class RolePermissionMapping {

    private final UUID id;
    private final String keycloakRole;
    private final Permission permission;

    public RolePermissionMapping(String keycloakRole, Permission permission) {
        this(UUID.randomUUID(), keycloakRole, permission);
    }

    public static RolePermissionMapping reconstitute(UUID id, String keycloakRole, Permission permission) {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(keycloakRole, "Keycloak role cannot be null");
        Objects.requireNonNull(permission, "Permission cannot be null");
        return new RolePermissionMapping(id, keycloakRole, permission);
    }
}
