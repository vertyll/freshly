package com.vertyll.freshly.useraccess.api.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

public record UpdateUserRolesRequestDto(
        @NotEmpty(message = "{validation.user.roles.required}") Set<String> roles) {}
