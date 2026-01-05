package com.vertyll.freshly.permission.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.vertyll.freshly.permission.Permission;
import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import com.vertyll.freshly.permission.domain.RolePermissionMappingRepository;

@ExtendWith(MockitoExtension.class)
class UserPermissionCacheTest {

    @Mock private RolePermissionMappingRepository rolePermissionRepository;

    @Mock private Authentication authentication;

    private UserPermissionCache userPermissionCache;

    @BeforeEach
    void setUp() {
        userPermissionCache = new UserPermissionCache(rolePermissionRepository);
    }

    @Test
    @DisplayName("Should get user permissions from repository")
    void shouldGetUserPermissionsFromRepository() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        Collection<GrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority("ROLE_admin"),
                        new SimpleGrantedAuthority("ROLE_user"));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping1 =
                new RolePermissionMapping(UUID.randomUUID(), "admin", Permission.USERS_READ);
        RolePermissionMapping mapping2 =
                new RolePermissionMapping(UUID.randomUUID(), "admin", Permission.USERS_CREATE);
        RolePermissionMapping mapping3 =
                new RolePermissionMapping(UUID.randomUUID(), "user", Permission.REPORTS_READ);

        when(rolePermissionRepository.findByKeycloakRoleIn(Set.of("admin", "user")))
                .thenReturn(List.of(mapping1, mapping2, mapping3));

        // When
        Set<Permission> permissions = userPermissionCache.getUserPermissions(authentication);

        // Then
        assertThat(permissions)
                .hasSize(3)
                .contains(Permission.USERS_READ, Permission.USERS_CREATE, Permission.REPORTS_READ);
        verify(rolePermissionRepository).findByKeycloakRoleIn(Set.of("admin", "user"));
    }

    @Test
    @DisplayName("Should strip ROLE_ prefix from authorities")
    void shouldStripRolePrefixFromAuthorities() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        Collection<GrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority("ROLE_admin"),
                        new SimpleGrantedAuthority("ROLE_moderator"));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping =
                new RolePermissionMapping(UUID.randomUUID(), "admin", Permission.USERS_READ);
        when(rolePermissionRepository.findByKeycloakRoleIn(any())).thenReturn(List.of(mapping));

        // When
        userPermissionCache.getUserPermissions(authentication);

        // Then
        verify(rolePermissionRepository).findByKeycloakRoleIn(Set.of("admin", "moderator"));
    }

    @Test
    @DisplayName("Should handle authorities without ROLE_ prefix")
    void shouldHandleAuthoritiesWithoutRolePrefix() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        Collection<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("admin"), new SimpleGrantedAuthority("user"));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping =
                new RolePermissionMapping(UUID.randomUUID(), "admin", Permission.USERS_READ);
        when(rolePermissionRepository.findByKeycloakRoleIn(any())).thenReturn(List.of(mapping));

        // When
        userPermissionCache.getUserPermissions(authentication);

        // Then
        verify(rolePermissionRepository).findByKeycloakRoleIn(Set.of("admin", "user"));
    }

    @Test
    @DisplayName("Should return empty set when user has no roles")
    void shouldReturnEmptySetWhenUserHasNoRoles() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.getAuthorities()).thenReturn(List.of());
        when(rolePermissionRepository.findByKeycloakRoleIn(Set.of())).thenReturn(List.of());

        // When
        Set<Permission> permissions = userPermissionCache.getUserPermissions(authentication);

        // Then
        assertThat(permissions).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when roles have no mapped permissions")
    void shouldReturnEmptySetWhenRolesHaveNoMappedPermissions() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        Collection<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_norole"));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);
        when(rolePermissionRepository.findByKeycloakRoleIn(Set.of("norole"))).thenReturn(List.of());

        // When
        Set<Permission> permissions = userPermissionCache.getUserPermissions(authentication);

        // Then
        assertThat(permissions).isEmpty();
    }

    @Test
    @DisplayName("Should deduplicate permissions from multiple roles")
    void shouldDeduplicatePermissionsFromMultipleRoles() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        Collection<GrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority("ROLE_admin"),
                        new SimpleGrantedAuthority("ROLE_superadmin"));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping1 =
                new RolePermissionMapping(UUID.randomUUID(), "admin", Permission.USERS_READ);
        RolePermissionMapping mapping2 =
                new RolePermissionMapping(UUID.randomUUID(), "superadmin", Permission.USERS_READ);
        RolePermissionMapping mapping3 =
                new RolePermissionMapping(UUID.randomUUID(), "superadmin", Permission.USERS_DELETE);

        when(rolePermissionRepository.findByKeycloakRoleIn(Set.of("admin", "superadmin")))
                .thenReturn(List.of(mapping1, mapping2, mapping3));

        // When
        Set<Permission> permissions = userPermissionCache.getUserPermissions(authentication);

        // Then
        assertThat(permissions).hasSize(2).contains(Permission.USERS_READ, Permission.USERS_DELETE);
    }

    @Test
    @DisplayName("Should filter out null authorities")
    void shouldFilterOutNullAuthorities() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        GrantedAuthority nullAuthority = () -> null;
        Collection<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_admin"), nullAuthority);
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping =
                new RolePermissionMapping(UUID.randomUUID(), "admin", Permission.USERS_READ);
        when(rolePermissionRepository.findByKeycloakRoleIn(any())).thenReturn(List.of(mapping));

        // When
        userPermissionCache.getUserPermissions(authentication);

        // Then
        verify(rolePermissionRepository).findByKeycloakRoleIn(Set.of("admin"));
    }

    @Test
    @DisplayName("Should handle mixed ROLE_ prefix authorities")
    void shouldHandleMixedRolePrefixAuthorities() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        Collection<GrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority("ROLE_admin"),
                        new SimpleGrantedAuthority("user"));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping1 =
                new RolePermissionMapping(UUID.randomUUID(), "admin", Permission.USERS_READ);
        RolePermissionMapping mapping2 =
                new RolePermissionMapping(UUID.randomUUID(), "user", Permission.REPORTS_READ);
        when(rolePermissionRepository.findByKeycloakRoleIn(Set.of("admin", "user")))
                .thenReturn(List.of(mapping1, mapping2));

        // When
        Set<Permission> permissions = userPermissionCache.getUserPermissions(authentication);

        // Then
        assertThat(permissions).hasSize(2).contains(Permission.USERS_READ, Permission.REPORTS_READ);
        verify(rolePermissionRepository).findByKeycloakRoleIn(Set.of("admin", "user"));
    }

    @Test
    @DisplayName("Should cache results per username")
    void shouldCacheResultsPerUsername() {
        // Given
        when(authentication.getName()).thenReturn("testuser");
        Collection<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_admin"));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping =
                new RolePermissionMapping(UUID.randomUUID(), "admin", Permission.USERS_READ);
        when(rolePermissionRepository.findByKeycloakRoleIn(Set.of("admin")))
                .thenReturn(List.of(mapping));

        Set<Permission> permissions1 = userPermissionCache.getUserPermissions(authentication);
        Set<Permission> permissions2 = userPermissionCache.getUserPermissions(authentication);

        assertThat(permissions1).isEqualTo(permissions2).contains(Permission.USERS_READ);
    }
}
