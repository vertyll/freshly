package com.vertyll.freshly.permission.application.dto;

import java.util.List;

public record PermissionModuleResponse(String context, List<DeclaredPermission> permissions) {

    public record DeclaredPermission(String value, String scope, String descriptionKey) {
    }

    public PermissionModuleResponse {
        permissions = List.copyOf(permissions);
    }
}
