package com.vertyll.freshly.permission.application.service.query;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vertyll.freshly.authz.CallerRoles;
import com.vertyll.freshly.authz.PermissionCatalogue;
import com.vertyll.freshly.authz.PermissionDescriptor;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.permission.application.dto.PermissionModuleResponse;
import com.vertyll.freshly.permission.application.dto.RoleAuthorityResponse;
import com.vertyll.freshly.permission.application.port.inbound.query.PermissionQueryUseCase;
import com.vertyll.freshly.permission.domain.error.PermissionError;
import com.vertyll.freshly.permission.domain.model.RoleAuthority;
import com.vertyll.freshly.permission.domain.model.RoleGrants;
import com.vertyll.freshly.permission.domain.repository.RoleAuthorityRepository;

public class PermissionQueryService implements PermissionQueryUseCase {
    private final RoleAuthorityRepository roles;
    private final List<PermissionCatalogue> catalogues;

    public PermissionQueryService(RoleAuthorityRepository roles, Collection<PermissionCatalogue> catalogues) {
        this.roles = roles;
        this.catalogues = List.copyOf(catalogues);
    }

    @Override
    public boolean permits(CallerRoles callerRoles, String permission) {
        return !callerRoles.isEmpty() && roles.grantsFor(callerRoles.values()).grants(permission);
    }

    @Override
    public boolean permitsAny(CallerRoles callerRoles, Set<String> permissions) {
        if (callerRoles.isEmpty() || permissions.isEmpty()) {
            return false;
        }
        RoleGrants held = roles.grantsFor(callerRoles.values());
        return permissions.stream().anyMatch(held::grants);
    }

    @Override
    public Set<String> permissionsOf(CallerRoles callerRoles) {
        if (callerRoles.isEmpty()) {
            return Set.of();
        }
        RoleGrants held = roles.grantsFor(callerRoles.values());

        return held.anyUnrestricted() ? declaredValues() : held.permissions();
    }

    @Override
    public List<RoleAuthorityResponse> listRoles() {
        return roles.findAll()
            .stream()
            .sorted(Comparator.comparing(RoleAuthority::role))
            .map(RoleAuthorityResponse::from)
            .toList();
    }

    @Override
    public RoleAuthorityResponse roleAuthority(String role) {
        String normalised = RoleAuthority.normalise(role);
        return roles.findByRole(normalised)
            .map(RoleAuthorityResponse::from)
            .orElseThrow(() -> new DomainException(PermissionError.ROLE_NOT_FOUND, Map.of("role", normalised)));
    }

    @Override
    public List<PermissionModuleResponse> declaredPermissions() {
        return catalogues.stream()
            .sorted(Comparator.comparing(PermissionCatalogue::context))
            .map(
                catalogue -> new PermissionModuleResponse(
                    catalogue.context(),
                    catalogue.permissions()
                        .stream()
                        .sorted(Comparator.comparing(PermissionDescriptor::value))
                        .map(
                            permission -> new PermissionModuleResponse.DeclaredPermission(
                                permission.value(),
                                permission.scope().name(),
                                permission.descriptionKey()
                            )
                        )
                        .toList()
                )
            )
            .toList();
    }

    private Set<String> declaredValues() {
        return catalogues.stream()
            .flatMap(catalogue -> catalogue.values().stream())
            .collect(Collectors.toUnmodifiableSet());
    }
}
