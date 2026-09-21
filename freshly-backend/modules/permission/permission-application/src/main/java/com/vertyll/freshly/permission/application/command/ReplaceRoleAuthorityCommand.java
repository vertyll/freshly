package com.vertyll.freshly.permission.application.command;

import java.util.Set;

import org.jspecify.annotations.Nullable;

public record ReplaceRoleAuthorityCommand(
    String role,
    boolean unrestricted,
    Set<String> permissions,
    @Nullable Long expectedVersion
) {
    public ReplaceRoleAuthorityCommand {
        permissions = Set.copyOf(permissions);
    }
}
