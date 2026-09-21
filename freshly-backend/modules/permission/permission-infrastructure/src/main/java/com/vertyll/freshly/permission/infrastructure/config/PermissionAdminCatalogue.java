package com.vertyll.freshly.permission.infrastructure.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.authz.PermissionCatalogue;
import com.vertyll.freshly.authz.PermissionDescriptor;
import com.vertyll.freshly.authz.StockRole;
import com.vertyll.freshly.permission.application.security.PermissionAdminPermission;

@Component
public class PermissionAdminCatalogue implements PermissionCatalogue {
    private static final String ADMIN_ROLE = "ADMIN";

    @Override
    public String context() {
        return PermissionAdminPermission.CONTEXT;
    }

    @Override
    public Set<PermissionDescriptor> permissions() {
        return Arrays.stream(PermissionAdminPermission.values())
            .map(PermissionDescriptor.class::cast)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<StockRole> stockRoles() {
        return Set.of(StockRole.unrestricted(ADMIN_ROLE));
    }
}
