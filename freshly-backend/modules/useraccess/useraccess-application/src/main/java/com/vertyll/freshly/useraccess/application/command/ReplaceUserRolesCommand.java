package com.vertyll.freshly.useraccess.application.command;

import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record ReplaceUserRolesCommand(UUID userId, Set<String> roles, @Nullable Long expectedVersion) {
    public ReplaceUserRolesCommand {
        roles = Set.copyOf(roles);
    }
}
