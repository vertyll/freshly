package com.vertyll.freshly.useraccess.infrastructure.web.dto;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.vertyll.freshly.useraccess.application.command.CreateUserCommand;

public record CreateUserRequestDto(@NotNull UUID keycloakUserId, boolean isActive, @NotEmpty Set<String> roles) {
    public CreateUserCommand toCommand() {
        return new CreateUserCommand(keycloakUserId, isActive, roles);
    }
}
