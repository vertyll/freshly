package com.vertyll.freshly.permission.infrastructure.web.dto;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.permission.application.command.ReplaceRoleAuthorityCommand;

public record ReplaceRoleAuthorityRequestDto(boolean unrestricted, @NotNull Set<String> permissions) {
    public ReplaceRoleAuthorityCommand toCommand(String role, @Nullable Long expectedVersion) {
        return new ReplaceRoleAuthorityCommand(role, unrestricted, permissions, expectedVersion);
    }
}
