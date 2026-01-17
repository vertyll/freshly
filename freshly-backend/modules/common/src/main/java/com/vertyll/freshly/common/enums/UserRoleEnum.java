package com.vertyll.freshly.common.enums;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import lombok.Getter;

@Getter
public enum UserRoleEnum {
    // Keycloak specific roles
    OFFLINE_ACCESS("OFFLINE_ACCESS"),
    FACTOR_BEARER("FACTOR_BEARER"),
    UMA_AUTHORIZATION("UMA_AUTHORIZATION"),
    DEFAULT_ROLES_FRESHLY_REALM("DEFAULT-ROLES-FRESHLY-REALM"),

    SUPERADMIN("SUPERADMIN"),
    ADMIN("ADMIN"),
    MODERATOR("MODERATOR"),
    USER("USER");

    public static final String ROLE_PREFIX = "ROLE_";

    private static final String UNKNOWN_USER_ROLE = "Unknown user role: ";

    private final String value;
    private final String roleWithPrefix;

    UserRoleEnum(String value) {
        this.value = value;
        this.roleWithPrefix = ROLE_PREFIX + value.toLowerCase(Locale.ROOT);
    }

    public static UserRoleEnum fromValue(@Nullable String value) {
        if (value == null) {
            throw new IllegalArgumentException(UNKNOWN_USER_ROLE + "null");
        }

        for (UserRoleEnum userRoleEnum : values()) {
            if (userRoleEnum.value.equals(value)) {
                return userRoleEnum;
            }
        }

        throw new IllegalArgumentException(UNKNOWN_USER_ROLE + value);
    }
}
