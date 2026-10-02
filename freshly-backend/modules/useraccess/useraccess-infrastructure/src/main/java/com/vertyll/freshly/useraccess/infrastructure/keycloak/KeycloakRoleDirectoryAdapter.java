package com.vertyll.freshly.useraccess.infrastructure.keycloak;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.useraccess.application.port.outbound.RoleDirectoryPort;
import com.vertyll.freshly.useraccess.infrastructure.config.KeycloakRealmProperties;

@Component
public class KeycloakRoleDirectoryAdapter implements RoleDirectoryPort {

    private static final String COMPOSITE_DEFAULT_PREFIX = "default-roles-";

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakRoleDirectoryAdapter(
        @Qualifier(KeycloakRoleDirectoryConfig.USERACCESS_KEYCLOAK) Keycloak keycloak,
        KeycloakRealmProperties properties
    ) {
        this.keycloak = keycloak;
        this.realm = properties.realm();
    }

    @Override
    public Set<String> availableRoles() {
        return keycloak.realm(realm)
            .roles()
            .list()
            .stream()
            .map(RoleRepresentation::getName)
            .filter(KeycloakRoleDirectoryAdapter::isAssignable)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void replaceRoles(UUID keycloakUserId, Set<String> roles) {
        RoleMappingResource mappings = keycloak.realm(realm).users().get(keycloakUserId.toString()).roles();

        List<RoleRepresentation> held =
                mappings.realmLevel().listAll().stream().filter(role -> isAssignable(role.getName())).toList();
        List<RoleRepresentation> wanted =
                roles.stream().map(role -> keycloak.realm(realm).roles().get(role).toRepresentation()).toList();

        List<RoleRepresentation> toRemove = held.stream().filter(role -> !roles.contains(role.getName())).toList();
        List<RoleRepresentation> toAdd = wanted.stream()
            .filter(role -> held.stream().noneMatch(existing -> existing.getName().equals(role.getName())))
            .toList();

        if (!toRemove.isEmpty()) {
            mappings.realmLevel().remove(toRemove);
        }
        if (!toAdd.isEmpty()) {
            mappings.realmLevel().add(toAdd);
        }
    }

    @Override
    public void setEnabled(UUID keycloakUserId, boolean enabled) {
        UserResource user = keycloak.realm(realm).users().get(keycloakUserId.toString());
        UserRepresentation representation = user.toRepresentation();
        representation.setEnabled(enabled);
        user.update(representation);
        if (!enabled) {
            user.logout();
        }
    }

    private static boolean isAssignable(String roleName) {
        return !roleName.startsWith(COMPOSITE_DEFAULT_PREFIX);
    }
}
