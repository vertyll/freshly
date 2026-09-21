package com.vertyll.freshly.permission.application.service.command;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.vertyll.freshly.authz.PermissionCatalogue;
import com.vertyll.freshly.authz.StockRole;
import com.vertyll.freshly.lang.concurrency.VersionGuard;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.UseCaseLogger;
import com.vertyll.freshly.permission.application.command.ReplaceRoleAuthorityCommand;
import com.vertyll.freshly.permission.application.dto.RoleAuthorityResponse;
import com.vertyll.freshly.permission.application.port.inbound.command.RoleAuthorityCommandUseCase;
import com.vertyll.freshly.permission.domain.error.PermissionError;
import com.vertyll.freshly.permission.domain.model.RoleAuthority;
import com.vertyll.freshly.permission.domain.repository.RoleAuthorityRepository;

public class RoleAuthorityCommandService implements RoleAuthorityCommandUseCase {
    private static final String ROLE = "role";

    private final RoleAuthorityRepository roles;
    private final Collection<PermissionCatalogue> catalogues;
    private final UseCaseLogger logger;

    public RoleAuthorityCommandService(
        RoleAuthorityRepository roles,
        Collection<PermissionCatalogue> catalogues,
        UseCaseLogger logger
    ) {
        this.roles = roles;
        this.catalogues = List.copyOf(catalogues);
        this.logger = logger;
    }

    @Override
    public RoleAuthorityResponse replace(ReplaceRoleAuthorityCommand command) {
        requireAllDeclared(command.permissions(), declaredPermissions());

        String role = RoleAuthority.normalise(command.role());
        RoleAuthority authority = roles.findByRole(role).orElseGet(() -> RoleAuthority.create(role, false, Set.of()));

        VersionGuard.requireMatch(
            authority.version(),
            command.expectedVersion(),
            () -> new DomainException(PermissionError.VERSION_MISMATCH, Map.of(ROLE, role))
        );

        authority.replaceWith(command.unrestricted(), command.permissions());
        RoleAuthority saved = roles.save(authority);

        logger.info(
            "Role {} now holds {} permissions (unrestricted={})",
            saved.role(),
            saved.permissions().size(),
            saved.unrestricted()
        );
        return RoleAuthorityResponse.from(saved);
    }

    @Override
    public void delete(String role) {
        String normalised = RoleAuthority.normalise(role);
        if (roles.findByRole(normalised).isEmpty()) {
            throw new DomainException(PermissionError.ROLE_NOT_FOUND, Map.of(ROLE, normalised));
        }

        roles.deleteByRole(normalised);
        logger.info("Role {} holds nothing any more", normalised);
    }

    @Override
    public int seedStockRoles(Collection<StockRole> stockRoles) {
        Set<String> declared = declaredPermissions();
        int applied = 0;

        for (StockRole stock : stockRoles) {
            String role = RoleAuthority.normalise(stock.role());
            Optional<RoleAuthority> existing = roles.findByRole(role);

            if (existing.filter(found -> !found.holdsNothing()).isPresent()) {
                continue;
            }
            requireAllDeclared(stock.permissions(), declared);

            RoleAuthority authority = existing.orElseGet(() -> RoleAuthority.create(role, false, Set.of()));
            authority.replaceWith(stock.unrestricted(), stock.permissions());
            roles.save(authority);
            applied++;

            logger.info("Seeded stock role {} (unrestricted={})", role, stock.unrestricted());
        }
        return applied;
    }

    private Set<String> declaredPermissions() {
        return catalogues.stream()
            .flatMap(catalogue -> catalogue.values().stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    private void requireAllDeclared(Set<String> permissions, Set<String> declared) {
        Set<String> unknown = permissions.stream()
            .filter(permission -> !declared.contains(permission))
            .collect(Collectors.toCollection(TreeSet::new));

        if (!unknown.isEmpty()) {
            throw new DomainException(PermissionError.UNKNOWN_PERMISSION, Map.of("permissions", unknown));
        }
    }
}
