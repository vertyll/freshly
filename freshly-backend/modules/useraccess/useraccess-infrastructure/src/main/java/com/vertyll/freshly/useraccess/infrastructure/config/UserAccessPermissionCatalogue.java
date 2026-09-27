package com.vertyll.freshly.useraccess.infrastructure.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.authz.PermissionCatalogue;
import com.vertyll.freshly.authz.PermissionDescriptor;
import com.vertyll.freshly.useraccess.application.security.UserAccessPermission;

@Component
public class UserAccessPermissionCatalogue implements PermissionCatalogue {
    @Override
    public String context() {
        return UserAccessPermission.CONTEXT_NAME;
    }

    @Override
    public Set<PermissionDescriptor> permissions() {
        return Arrays.stream(UserAccessPermission.values())
            .map(PermissionDescriptor.class::cast)
            .collect(Collectors.toUnmodifiableSet());
    }
}
