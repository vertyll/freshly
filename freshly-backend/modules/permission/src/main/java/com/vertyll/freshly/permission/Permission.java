package com.vertyll.freshly.permission;

import lombok.Getter;

/**
 * Defines all permissions in the system. Permissions are granular capabilities that can be assigned
 * to roles.
 */
@Getter
public enum Permission {

    // User Management
    USERS_READ("users:read"),
    USERS_CREATE("users:create"),
    USERS_UPDATE("users:update"),
    USERS_DELETE("users:delete"),
    USERS_ACTIVATE("users:activate"),
    USERS_DEACTIVATE("users:deactivate"),
    USERS_MANAGE_ROLES("users:manage-roles"),

    // Auth Operations
    AUTH_CHANGE_PASSWORD("auth:change-password"),
    AUTH_CHANGE_EMAIL("auth:change-email"),

    // Reports
    REPORTS_READ("reports:read"),
    REPORTS_GENERATE("reports:generate"),
    REPORTS_DELETE("reports:delete"),

    // Settings
    SETTINGS_MANAGE("settings:manage");

    private final String value;

    Permission(String value) {
        this.value = value;
    }

    public static Permission fromValue(String value) {
        for (Permission permission : values()) {
            if (permission.value.equals(value)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("Unknown permission: " + value);
    }
}
