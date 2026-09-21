package com.vertyll.freshly.useraccess.infrastructure.web.dto;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.useraccess.application.command.ReplaceUserRolesCommand;

public record UpdateUserRolesRequestDto(@NotEmpty Set<String> roles) {

    public ReplaceUserRolesCommand toCommand(UUID userId, @Nullable Long expectedVersion) {
        return new ReplaceUserRolesCommand(userId, roles, expectedVersion);
    }
}
