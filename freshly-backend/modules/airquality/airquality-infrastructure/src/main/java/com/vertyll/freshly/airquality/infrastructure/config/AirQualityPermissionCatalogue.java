package com.vertyll.freshly.airquality.infrastructure.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.airquality.application.security.AirQualityPermission;
import com.vertyll.freshly.authz.PermissionCatalogue;
import com.vertyll.freshly.authz.PermissionDescriptor;

@Component
public class AirQualityPermissionCatalogue implements PermissionCatalogue {

    @Override
    public String context() {
        return AirQualityPermission.CONTEXT;
    }

    @Override
    public Set<PermissionDescriptor> permissions() {
        return Arrays.stream(AirQualityPermission.values())
            .map(PermissionDescriptor.class::cast)
            .collect(Collectors.toUnmodifiableSet());
    }
}
