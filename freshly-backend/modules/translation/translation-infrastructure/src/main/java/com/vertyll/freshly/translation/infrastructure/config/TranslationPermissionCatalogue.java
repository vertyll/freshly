package com.vertyll.freshly.translation.infrastructure.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.authz.PermissionCatalogue;
import com.vertyll.freshly.authz.PermissionDescriptor;
import com.vertyll.freshly.translation.application.security.TranslationPermission;

@Component
public class TranslationPermissionCatalogue implements PermissionCatalogue {

    @Override
    public String context() {
        return TranslationPermission.CONTEXT;
    }

    @Override
    public Set<PermissionDescriptor> permissions() {
        return Arrays.stream(TranslationPermission.values())
            .map(PermissionDescriptor.class::cast)
            .collect(Collectors.toUnmodifiableSet());
    }
}
