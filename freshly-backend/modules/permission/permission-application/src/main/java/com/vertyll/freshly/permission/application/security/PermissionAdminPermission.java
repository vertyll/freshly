package com.vertyll.freshly.permission.application.security;

import com.vertyll.freshly.authz.PermissionDescriptor;
import com.vertyll.freshly.authz.PermissionScope;

public enum PermissionAdminPermission implements PermissionDescriptor {
    PERMISSIONS_READ(Values.PERMISSIONS_READ),
    PERMISSIONS_MANAGE(Values.PERMISSIONS_MANAGE);

    public static final String CONTEXT_NAME = "permission";

    private final String value;

    PermissionAdminPermission(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public PermissionScope scope() {
        return PermissionScope.GLOBAL;
    }

    @Override
    public String context() {
        return CONTEXT_NAME;
    }

    public static final class Values {
        public static final String PERMISSIONS_READ = "permissions:read";
        public static final String PERMISSIONS_MANAGE = "permissions:manage";

        private Values() {
        }
    }
}
