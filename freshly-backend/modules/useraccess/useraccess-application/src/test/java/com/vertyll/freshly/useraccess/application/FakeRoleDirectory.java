package com.vertyll.freshly.useraccess.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.useraccess.application.port.outbound.RoleDirectoryPort;

public final class FakeRoleDirectory implements RoleDirectoryPort {
    private final Set<String> available;
    private final Map<UUID, Set<String>> assigned = new LinkedHashMap<>();
    private final Map<UUID, Boolean> enabled = new LinkedHashMap<>();

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

    @Override
    public void setEnabled(UUID keycloakUserId, boolean enabled) {
        this.enabled.put(keycloakUserId, enabled);
    }

    public @Nullable Boolean enabledOf(UUID keycloakUserId) {
        return enabled.get(keycloakUserId);
    }

    public Set<String> rolesOf(UUID keycloakUserId) {
        return assigned.getOrDefault(keycloakUserId, Set.of());
    }
}
