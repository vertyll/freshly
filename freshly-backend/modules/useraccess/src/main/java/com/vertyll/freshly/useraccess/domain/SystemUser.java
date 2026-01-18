package com.vertyll.freshly.useraccess.domain;

import java.util.*;

import org.jspecify.annotations.Nullable;

import lombok.Getter;

import com.vertyll.freshly.useraccess.domain.exception.*;

@Getter
public final class SystemUser {
    private static final String KEYCLOAK_USER_ID_CANNOT_BE_NULL = "Keycloak user ID cannot be null";
    private static final String ROLES_CANNOT_BE_NULL = "Roles cannot be null";

    private final UUID keycloakUserId;
    private boolean isActive;
    private Set<String> roles;
    @Nullable private final Long version;

    public SystemUser(UUID keycloakUserId, boolean isActive, Set<String> roles) {
        this(keycloakUserId, isActive, roles, null);
    }

    private SystemUser(
            UUID keycloakUserId, boolean isActive, Set<String> roles, @Nullable Long version) {
        this.keycloakUserId =
                Objects.requireNonNull(keycloakUserId, KEYCLOAK_USER_ID_CANNOT_BE_NULL);
        this.roles = Set.copyOf(Objects.requireNonNull(roles, ROLES_CANNOT_BE_NULL));
        if (this.roles.isEmpty()) {
            throw new UserRolesEmptyException();
        }
        this.isActive = isActive;
        this.version = version;
    }

    public static SystemUser reconstitute(
            UUID keycloakUserId, boolean isActive, Set<String> roles, Long version) {
        return new SystemUser(keycloakUserId, isActive, roles, version);
    }

    public void activate() {
        if (isActive) {
            throw new UserAlreadyActiveException(keycloakUserId);
        }
        isActive = true;
    }

    public void deactivate(UUID loggedInUserId) {
        if (!isActive) {
            throw new UserAlreadyInactiveException(keycloakUserId);
        }
        if (Objects.equals(keycloakUserId, loggedInUserId)) {
            throw new SelfDeactivationException(loggedInUserId);
        }
        isActive = false;
    }

    public void deactivateSelf() {
        if (!isActive) {
            throw new UserAlreadyInactiveException(keycloakUserId);
        }
        isActive = false;
    }

    public void replaceRoles(Set<String> newRoles) {
        Set<String> copiedRoles =
                Set.copyOf(Objects.requireNonNull(newRoles, ROLES_CANNOT_BE_NULL));
        if (copiedRoles.isEmpty()) {
            throw new UserRolesEmptyException();
        }
        this.roles = copiedRoles;
    }

    public Set<String> getRoles() {
        return Set.copyOf(roles);
    }
}
