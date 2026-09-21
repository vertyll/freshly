package com.vertyll.freshly.useraccess.application.command;

import java.util.Set;
import java.util.UUID;

public record CreateUserCommand(UUID keycloakUserId, boolean active, Set<String> roles) {
    public CreateUserCommand {
        roles = Set.copyOf(roles);
    }
}
