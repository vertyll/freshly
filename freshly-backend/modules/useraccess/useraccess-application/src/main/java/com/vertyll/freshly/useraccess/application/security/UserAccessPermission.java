package com.vertyll.freshly.useraccess.application.security;

import com.vertyll.freshly.authz.PermissionDescriptor;
import com.vertyll.freshly.authz.PermissionScope;

public enum UserAccessPermission implements PermissionDescriptor {
    USERS_READ("users:read"),
    USERS_CREATE("users:create"),
    USERS_UPDATE("users:update"),
    USERS_DELETE("users:delete"),
    USERS_ACTIVATE("users:activate"),
    USERS_DEACTIVATE("users:deactivate"),
    USERS_MANAGE_ROLES("users:manageRoles");

    public static final String CONTEXT = "useraccess";

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
        return CONTEXT;
    }

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
