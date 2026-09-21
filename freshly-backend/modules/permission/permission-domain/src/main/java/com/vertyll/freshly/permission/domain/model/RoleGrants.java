package com.vertyll.freshly.permission.domain.model;

import java.util.Set;

public record RoleGrants(boolean anyUnrestricted, Set<String> permissions) {

    public static final RoleGrants NOTHING = new RoleGrants(false, Set.of());

    public RoleGrants {
        permissions = Set.copyOf(permissions);
    }

    public boolean grants(String permission) {
        return anyUnrestricted || permissions.contains(permission);
    }
}
