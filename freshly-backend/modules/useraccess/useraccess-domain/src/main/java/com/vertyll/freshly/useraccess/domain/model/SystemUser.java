package com.vertyll.freshly.useraccess.domain.model;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.useraccess.domain.error.UserAccessError;

import static java.util.Objects.requireNonNull;

public final class SystemUser {
    private static final String USER_ID_PARAM = "userId";

    private static final String KEYCLOAK_USER_ID_CANNOT_BE_NULL = "Keycloak user ID cannot be null";
    private static final String ROLES_CANNOT_BE_NULL = "Roles cannot be null";

    private final UUID keycloakUserId;
    private boolean active;
    private Set<String> roles;
    @Nullable private final Long version;

    private SystemUser(UUID keycloakUserId, boolean active, Set<String> roles, @Nullable Long version) {
        this.keycloakUserId = requireNonNull(keycloakUserId, KEYCLOAK_USER_ID_CANNOT_BE_NULL);
        this.roles = requireNonBlankRoles(roles);
        this.active = active;
        this.version = version;
    }

    public static SystemUser create(UUID keycloakUserId, boolean active, Set<String> roles) {
        return new SystemUser(keycloakUserId, active, roles, null);
    }

    public static SystemUser reconstitute(
        UUID keycloakUserId,
        boolean active,
        Set<String> roles,
        @Nullable Long version
    ) {
        return new SystemUser(keycloakUserId, active, roles, version);
    }

    public void activate() {
        if (active) {
            throw new DomainException(UserAccessError.USER_ALREADY_ACTIVE, Map.of(USER_ID_PARAM, keycloakUserId));
        }
        active = true;
    }

    public void deactivateBy(UUID actorId) {
        if (Objects.equals(keycloakUserId, actorId)) {
            throw new DomainException(UserAccessError.SELF_DEACTIVATION, Map.of(USER_ID_PARAM, actorId));
        }
        deactivate();
    }

    public void deactivate() {
        if (!active) {
            throw new DomainException(UserAccessError.USER_ALREADY_INACTIVE, Map.of(USER_ID_PARAM, keycloakUserId));
        }
        active = false;
    }

    public void replaceRoles(Set<String> newRoles) {
        this.roles = requireNonBlankRoles(newRoles);
    }

    public UUID keycloakUserId() {
        return keycloakUserId;
    }

    public boolean isActive() {
        return active;
    }

    public Set<String> roles() {
        return Set.copyOf(roles);
    }

    @Nullable public Long version() {
        return version;
    }

    private static Set<String> requireNonBlankRoles(Set<String> candidate) {
        Set<String> copied = Set.copyOf(requireNonNull(candidate, ROLES_CANNOT_BE_NULL));
        if (copied.isEmpty()) {
            throw new DomainException(UserAccessError.USER_ROLES_EMPTY);
        }
        return copied;
    }
}
