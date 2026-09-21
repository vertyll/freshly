package com.vertyll.freshly.permission.domain.model;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.permission.domain.error.PermissionError;

import static java.util.Objects.requireNonNull;

public final class RoleAuthority {
    private static final String ROLE_NULL = "Role cannot be null";
    private static final String PERMISSIONS_NULL = "Permissions cannot be null";

    private final String role;
    private boolean unrestricted;

    private final Set<String> permissions;
    @Nullable private final Long version;

    private RoleAuthority(String role, boolean unrestricted, Set<String> permissions, @Nullable Long version) {
        this.role = normalise(role);
        this.unrestricted = unrestricted;
        this.permissions = new LinkedHashSet<>(requireNonNull(permissions, PERMISSIONS_NULL));
        this.version = version;
    }

    public static RoleAuthority create(String role, boolean unrestricted, Set<String> permissions) {
        return new RoleAuthority(role, unrestricted, permissions, null);
    }

    public static RoleAuthority reconstitute(
        String role,
        boolean unrestricted,
        Set<String> permissions,
        @Nullable Long version
    ) {
        return new RoleAuthority(role, unrestricted, permissions, version);
    }

    public void replaceWith(boolean nowUnrestricted, Set<String> newPermissions) {
        requireNonNull(newPermissions, PERMISSIONS_NULL);

        unrestricted = nowUnrestricted;
        permissions.clear();
        permissions.addAll(newPermissions);
    }

    public boolean holdsNothing() {
        return !unrestricted && permissions.isEmpty();
    }

    public String role() {
        return role;
    }

    public boolean unrestricted() {
        return unrestricted;
    }

    public Set<String> permissions() {
        return Set.copyOf(permissions);
    }

    @Nullable public Long version() {
        return version;
    }

    public static String normalise(String candidate) {
        String value = requireNonNull(candidate, ROLE_NULL).trim();
        if (value.isEmpty()) {
            throw new DomainException(PermissionError.BLANK_ROLE);
        }
        return value.toUpperCase(Locale.ROOT);
    }
}
