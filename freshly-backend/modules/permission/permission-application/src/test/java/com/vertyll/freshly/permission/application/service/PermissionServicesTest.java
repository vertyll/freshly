package com.vertyll.freshly.permission.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.authz.CallerRoles;
import com.vertyll.freshly.authz.PermissionCatalogue;
import com.vertyll.freshly.authz.PermissionDescriptor;
import com.vertyll.freshly.authz.PermissionScope;
import com.vertyll.freshly.authz.StockRole;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.UseCaseLogger;
import com.vertyll.freshly.permission.application.command.ReplaceRoleAuthorityCommand;
import com.vertyll.freshly.permission.application.service.command.RoleAuthorityCommandService;
import com.vertyll.freshly.permission.application.service.query.PermissionQueryService;
import com.vertyll.freshly.permission.domain.error.PermissionError;
import com.vertyll.freshly.permission.domain.model.RoleAuthority;
import com.vertyll.freshly.permission.domain.model.RoleGrants;
import com.vertyll.freshly.permission.domain.repository.RoleAuthorityRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionServicesTest {
    private static final String READ = "users:read";
    private static final String DELETE = "users:delete";
    private static final String UNDECLARED = "users:teleport";

    private InMemoryRoles roles;
    private PermissionQueryService queries;
    private RoleAuthorityCommandService commands;

    @BeforeEach
    void setUp() {
        roles = new InMemoryRoles();
        List<PermissionCatalogue> catalogues = List.of(new FakeCatalogue(Set.of(READ, DELETE)));
        queries = new PermissionQueryService(roles, catalogues);
        commands = new RoleAuthorityCommandService(roles, catalogues, new NoOpLogger());
    }

    @Nested
    @DisplayName("the decision")
    class Decision {
        @Test
        @DisplayName("permits a role that holds the permission")
        void permitsGranted() {
            roles.hold("ADMIN", false, Set.of(READ));

            assertThat(queries.permits(CallerRoles.of(Set.of("ADMIN")), READ)).isTrue();
        }

        @Test
        @DisplayName("refuses a role that does not")
        void refusesUngranted() {
            roles.hold("ADMIN", false, Set.of(READ));

            assertThat(queries.permits(CallerRoles.of(Set.of("USER")), READ)).isFalse();
        }

        @Test
        @DisplayName("an anonymous caller holds nothing")
        void refusesAnonymous() {
            assertThat(queries.permits(CallerRoles.of(Set.of()), READ)).isFalse();
        }

        @Test
        @DisplayName("role names are matched without regard to case or padding")
        void normalisesRoleNames() {
            roles.hold("ADMIN", false, Set.of(READ));

            assertThat(queries.permits(CallerRoles.of(Set.of(" Admin ")), READ)).isTrue();
        }

        @Test
        @DisplayName("any one of several permissions is enough")
        void permitsAny() {
            roles.hold("EDITOR", false, Set.of(READ));

            assertThat(queries.permitsAny(CallerRoles.of(Set.of("EDITOR")), Set.of(READ, DELETE))).isTrue();
        }

        @Test
        @DisplayName("what a caller holds is the union over its roles")
        void unionOverRoles() {
            roles.hold("READER", false, Set.of(READ));
            roles.hold("REMOVER", false, Set.of(DELETE));

            assertThat(queries.permissionsOf(CallerRoles.of(Set.of("READER", "REMOVER"))))
                .containsExactlyInAnyOrder(READ, DELETE);
        }
    }

    @Nested
    @DisplayName("an unrestricted role")
    class Unrestricted {
        @Test
        @DisplayName("holds a permission it was never granted")
        void holdsEverything() {
            roles.hold("ADMIN", true, Set.of());

            assertThat(queries.permits(CallerRoles.of(Set.of("ADMIN")), DELETE)).isTrue();
        }

        @Test
        @DisplayName("reports the whole declared catalogue, so a module added later is covered")
        void reportsEverythingDeclared() {
            roles.hold("ADMIN", true, Set.of());

            assertThat(queries.permissionsOf(CallerRoles.of(Set.of("ADMIN")))).containsExactlyInAnyOrder(READ, DELETE);
        }
    }

    @Nested
    @DisplayName("editing a role")
    class Editing {
        @Test
        @DisplayName("replaces the whole set rather than adding to it")
        void replacesTheSet() {
            roles.hold("EDITOR", false, Set.of(READ, DELETE));

            commands.replace(new ReplaceRoleAuthorityCommand("EDITOR", false, Set.of(READ), null));

            assertThat(queries.permissionsOf(CallerRoles.of(Set.of("EDITOR")))).containsExactly(READ);
        }

        @Test
        @DisplayName("refuses a permission no module declares")
        void refusesUndeclared() {
            assertThatThrownBy(
                () -> commands.replace(new ReplaceRoleAuthorityCommand("EDITOR", false, Set.of(UNDECLARED), null))
            ).extracting(e -> ((DomainException) e).error()).isEqualTo(PermissionError.UNKNOWN_PERMISSION);
        }

        @Test
        @DisplayName("creates the role when it holds nothing yet")
        void createsOnFirstGrant() {
            commands.replace(new ReplaceRoleAuthorityCommand("SUPPORT", false, Set.of(READ), null));

            assertThat(queries.permits(CallerRoles.of(Set.of("SUPPORT")), READ)).isTrue();
        }

        @Test
        @DisplayName("deleting a role that holds nothing is a not-found")
        void deletingUnknownRole() {
            assertThatThrownBy(() -> commands.delete("NOBODY")).extracting(e -> ((DomainException) e).error())
                .isEqualTo(PermissionError.ROLE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("stock roles")
    class StockRoles {
        @Test
        @DisplayName("are created on an installation that has none")
        void seedsOnFirstBoot() {
            int created = commands.seedStockRoles(List.of(StockRole.unrestricted("ADMIN")));

            assertThat(created).isEqualTo(1);
            assertThat(queries.permits(CallerRoles.of(Set.of("ADMIN")), DELETE)).isTrue();
        }

        @Test
        @DisplayName("never restore a permission an administrator took away")
        void doesNotUndoAnAdministrator() {
            commands.seedStockRoles(List.of(StockRole.granting("SUPPORT", READ, DELETE)));
            commands.replace(new ReplaceRoleAuthorityCommand("SUPPORT", false, Set.of(READ), null));

            commands.seedStockRoles(List.of(StockRole.granting("SUPPORT", READ, DELETE)));

            assertThat(queries.permissionsOf(CallerRoles.of(Set.of("SUPPORT")))).containsExactly(READ);
        }

        @Test
        @DisplayName("are applied again to a role that was emptied")
        void reseedsAnEmptyRole() {
            roles.hold("SUPPORT", false, Set.of());

            int created = commands.seedStockRoles(List.of(StockRole.granting("SUPPORT", READ)));

            assertThat(created).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("the declared catalogue")
    class Declared {
        @Test
        @DisplayName("is grouped by the module that owns it, with a description key")
        void groupsByModule() {
            assertThat(queries.declaredPermissions()).hasSize(1);
            assertThat(queries.declaredPermissions().get(0).context()).isEqualTo("useraccess");
            assertThat(queries.declaredPermissions().get(0).permissions().get(0).descriptionKey())
                .isEqualTo("permission.users.delete");
        }
    }

    private static final class FakeCatalogue implements PermissionCatalogue {
        private final Set<String> values;

        private FakeCatalogue(Set<String> values) {
            this.values = values;
        }

        @Override
        public String context() {
            return "useraccess";
        }

        @Override
        public Set<PermissionDescriptor> permissions() {
            return values.stream()
                .map(value -> (PermissionDescriptor) new FakePermission(value))
                .collect(Collectors.toUnmodifiableSet());
        }
    }

    private record FakePermission(String value) implements PermissionDescriptor {
        @Override
        public PermissionScope scope() {
            return PermissionScope.GLOBAL;
        }

        @Override
        public String context() {
            return "useraccess";
        }
    }

    private static final class InMemoryRoles implements RoleAuthorityRepository {
        private final Map<String, RoleAuthority> stored = new LinkedHashMap<>();

        void hold(String role, boolean unrestricted, Set<String> permissions) {
            save(RoleAuthority.create(role, unrestricted, permissions));
        }

        @Override
        public RoleAuthority save(RoleAuthority roleAuthority) {
            stored.put(roleAuthority.role(), roleAuthority);
            return roleAuthority;
        }

        @Override
        public Optional<RoleAuthority> findByRole(String role) {
            return Optional.ofNullable(stored.get(role));
        }

        @Override
        public RoleGrants grantsFor(Set<String> callerRoles) {
            List<RoleAuthority> found = callerRoles.stream().map(stored::get).filter(Objects::nonNull).toList();

            Set<String> permissions = new LinkedHashSet<>();
            found.forEach(authority -> permissions.addAll(authority.permissions()));

            return new RoleGrants(found.stream().anyMatch(RoleAuthority::unrestricted), permissions);
        }

        @Override
        public List<RoleAuthority> findAll() {
            return new ArrayList<>(stored.values());
        }

        @Override
        public void deleteByRole(String role) {
            stored.remove(role);
        }
    }

    private static final class NoOpLogger implements UseCaseLogger {
        @Override
        public void debug(String message, Object... args) {
        }

        @Override
        public void info(String message, Object... args) {
        }

        @Override
        public void warn(String message, Object... args) {
        }

        @Override
        public void error(String message, Object... args) {
        }
    }
}
