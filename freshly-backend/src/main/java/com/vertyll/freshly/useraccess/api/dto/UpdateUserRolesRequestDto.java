package com.vertyll.freshly.useraccess.api.dto;

import com.vertyll.freshly.useraccess.domain.UserRoleEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateUserRolesRequestDto(
        @NotNull(message = "Roles cannot be null")
        @NotEmpty(message = "Roles cannot be empty")
        Set<UserRoleEnum> roles
) {
}
