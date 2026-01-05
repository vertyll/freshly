package com.vertyll.freshly.useraccess.domain;

import java.util.*;

import com.vertyll.freshly.useraccess.domain.exception.*;

import lombok.Getter;

@Getter
public final class SystemUser {

    private final UUID keycloakUserId;
    private boolean isActive;
    private Set<UserRoleEnum> roles;

    public SystemUser(UUID keycloakUserId, boolean isActive, Set<UserRoleEnum> roles) {
        this.keycloakUserId =
                Objects.requireNonNull(keycloakUserId, "Keycloak user ID cannot be null");
        this.roles = Set.copyOf(Objects.requireNonNull(roles, "Roles cannot be null"));
        if (this.roles.isEmpty()) {
            throw new UserRolesEmptyException();
        }
        this.isActive = isActive;
    }

    public static SystemUser reconstitute(
            UUID keycloakUserId, boolean isActive, Set<UserRoleEnum> roles) {
        SystemUser user = new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER));
        user.isActive = isActive;
        user.roles = Set.copyOf(roles);
        return user;
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

    public void replaceRoles(Set<UserRoleEnum> newRoles) {
        Set<UserRoleEnum> copiedRoles =
                Set.copyOf(Objects.requireNonNull(newRoles, "Roles cannot be null"));
        if (copiedRoles.isEmpty()) {
            throw new UserRolesEmptyException();
        }
        this.roles = copiedRoles;
    }

    public Set<UserRoleEnum> getRoles() {
        return Set.copyOf(roles);
    }
}
