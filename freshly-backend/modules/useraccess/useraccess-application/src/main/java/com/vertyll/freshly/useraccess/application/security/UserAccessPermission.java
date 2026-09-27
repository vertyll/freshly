package com.vertyll.freshly.useraccess.application.security;

import com.vertyll.freshly.authz.PermissionDescriptor;
import com.vertyll.freshly.authz.PermissionScope;

public enum UserAccessPermission implements PermissionDescriptor {
    USERS_READ(Values.USERS_READ),
    USERS_CREATE(Values.USERS_CREATE),
    USERS_UPDATE(Values.USERS_UPDATE),
    USERS_DELETE(Values.USERS_DELETE),
    USERS_ACTIVATE(Values.USERS_ACTIVATE),
    USERS_DEACTIVATE(Values.USERS_DEACTIVATE),
    USERS_MANAGE_ROLES(Values.USERS_MANAGE_ROLES);

    public static final String CONTEXT_NAME = "useraccess";

    private final String value;

    UserAccessPermission(String value) {
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

    @SuppressWarnings("PMD.DataClass")
    public static final class Values {
        public static final String USERS_READ = "users:read";
        public static final String USERS_CREATE = "users:create";
        public static final String USERS_UPDATE = "users:update";
        public static final String USERS_DELETE = "users:delete";
        public static final String USERS_ACTIVATE = "users:activate";
        public static final String USERS_DEACTIVATE = "users:deactivate";
        public static final String USERS_MANAGE_ROLES = "users:manageRoles";

        private Values() {
        }
    }
}
