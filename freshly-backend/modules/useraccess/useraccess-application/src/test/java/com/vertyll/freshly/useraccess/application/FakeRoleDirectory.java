package com.vertyll.freshly.useraccess.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.vertyll.freshly.useraccess.application.port.outbound.RoleDirectoryPort;

public final class FakeRoleDirectory implements RoleDirectoryPort {
    private final Set<String> available;
    private final Map<UUID, Set<String>> assigned = new LinkedHashMap<>();

    public FakeRoleDirectory(Set<String> available) {
        this.available = Set.copyOf(available);
    }

    @Override
    public Set<String> availableRoles() {
        return available;
    }

    @Override
    public void replaceRoles(UUID keycloakUserId, Set<String> roles) {
        assigned.put(keycloakUserId, Set.copyOf(roles));
    }

    public Set<String> rolesOf(UUID keycloakUserId) {
        return assigned.getOrDefault(keycloakUserId, Set.of());
    }
}
