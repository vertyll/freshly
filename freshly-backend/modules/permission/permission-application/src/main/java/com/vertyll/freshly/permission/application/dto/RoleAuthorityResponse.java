package com.vertyll.freshly.permission.application.dto;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.permission.domain.model.RoleAuthority;

public record RoleAuthorityResponse(
    String role,
    boolean unrestricted,
    List<String> permissions,
    @Nullable Long version
) {

    public RoleAuthorityResponse {
        permissions = List.copyOf(permissions);
    }

    public static RoleAuthorityResponse from(RoleAuthority authority) {
        Set<String> held = authority.permissions();
        return new RoleAuthorityResponse(
            authority.role(),
            authority.unrestricted(),
            held.stream().sorted().toList(),
            authority.version()
        );
    }
}
