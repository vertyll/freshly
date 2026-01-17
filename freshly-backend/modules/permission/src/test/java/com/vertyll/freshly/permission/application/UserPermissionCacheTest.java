package com.vertyll.freshly.permission.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.permission.Permission;
import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import com.vertyll.freshly.permission.domain.RolePermissionMappingRepository;

@ExtendWith(MockitoExtension.class)
class UserPermissionCacheTest {
    private static final String TEST_USER = "testuser";

    @Mock
    @SuppressWarnings("NullAway.Init")
    private RolePermissionMappingRepository rolePermissionRepository;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private Authentication authentication;

    private UserPermissionCache userPermissionCache;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        userPermissionCache = new UserPermissionCache(rolePermissionRepository);
    }

    @Test
    @DisplayName("Should get user permissions from repository")
    void shouldGetUserPermissionsFromRepository() {
        // Given
        when(authentication.getName()).thenReturn(TEST_USER);
        Collection<GrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority(UserRoleEnum.ADMIN.getRoleWithPrefix()),
                        new SimpleGrantedAuthority(UserRoleEnum.USER.getRoleWithPrefix()));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping1 =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_READ);
        RolePermissionMapping mapping2 =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_CREATE);
        RolePermissionMapping mapping3 =
                new RolePermissionMapping(UserRoleEnum.USER, Permission.REPORTS_READ);

        when(rolePermissionRepository.findByKeycloakRoleIn(
                        Set.of(UserRoleEnum.ADMIN, UserRoleEnum.USER)))
                .thenReturn(List.of(mapping1, mapping2, mapping3));

        // When
        Set<Permission> permissions = userPermissionCache.getUserPermissions(authentication);

        // Then
        assertThat(permissions)
                .hasSize(3)
                .contains(Permission.USERS_READ, Permission.USERS_CREATE, Permission.REPORTS_READ);
        verify(rolePermissionRepository)
                .findByKeycloakRoleIn(Set.of(UserRoleEnum.ADMIN, UserRoleEnum.USER));
    }

    @Test
    @DisplayName("Should strip ROLE_ prefix from authorities")
    void shouldStripRolePrefixFromAuthorities() {
        // Given
        when(authentication.getName()).thenReturn(TEST_USER);
        Collection<GrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority(UserRoleEnum.ADMIN.getRoleWithPrefix()),
                        new SimpleGrantedAuthority(UserRoleEnum.MODERATOR.getRoleWithPrefix()));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_READ);
        when(rolePermissionRepository.findByKeycloakRoleIn(any())).thenReturn(List.of(mapping));

        // When
        userPermissionCache.getUserPermissions(authentication);

        // Then
        verify(rolePermissionRepository)
                .findByKeycloakRoleIn(Set.of(UserRoleEnum.ADMIN, UserRoleEnum.MODERATOR));
    }

    @Test
    @DisplayName("Should handle authorities without ROLE_ prefix")
    void shouldHandleAuthoritiesWithoutRolePrefix() {
        // Given
        when(authentication.getName()).thenReturn(TEST_USER);
        Collection<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("admin"), new SimpleGrantedAuthority("user"));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_READ);
        when(rolePermissionRepository.findByKeycloakRoleIn(any())).thenReturn(List.of(mapping));

        // When
        userPermissionCache.getUserPermissions(authentication);

        // Then
        verify(rolePermissionRepository)
                .findByKeycloakRoleIn(Set.of(UserRoleEnum.ADMIN, UserRoleEnum.USER));
    }

    @Test
    @DisplayName("Should return empty set when user has no roles")
    void shouldReturnEmptySetWhenUserHasNoRoles() {
        // Given
        when(authentication.getName()).thenReturn(TEST_USER);
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
        when(authentication.getName()).thenReturn(TEST_USER);
        Collection<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(UserRoleEnum.USER.getRoleWithPrefix()));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);
        when(rolePermissionRepository.findByKeycloakRoleIn(Set.of(UserRoleEnum.USER)))
                .thenReturn(List.of());

        // When
        Set<Permission> permissions = userPermissionCache.getUserPermissions(authentication);

        // Then
        assertThat(permissions).isEmpty();
    }

    @Test
    @DisplayName("Should deduplicate permissions from multiple roles")
    void shouldDeduplicatePermissionsFromMultipleRoles() {
        // Given
        when(authentication.getName()).thenReturn(TEST_USER);
        Collection<GrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority(UserRoleEnum.ADMIN.getRoleWithPrefix()),
                        new SimpleGrantedAuthority(UserRoleEnum.SUPERADMIN.getRoleWithPrefix()));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping1 =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_READ);
        RolePermissionMapping mapping2 =
                new RolePermissionMapping(UserRoleEnum.SUPERADMIN, Permission.USERS_READ);
        RolePermissionMapping mapping3 =
                new RolePermissionMapping(UserRoleEnum.SUPERADMIN, Permission.USERS_DELETE);

        when(rolePermissionRepository.findByKeycloakRoleIn(
                        Set.of(UserRoleEnum.ADMIN, UserRoleEnum.SUPERADMIN)))
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
        when(authentication.getName()).thenReturn(TEST_USER);
        GrantedAuthority nullAuthority = () -> null;
        Collection<GrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority(UserRoleEnum.ADMIN.getRoleWithPrefix()),
                        nullAuthority);
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_READ);
        when(rolePermissionRepository.findByKeycloakRoleIn(any())).thenReturn(List.of(mapping));

        // When
        userPermissionCache.getUserPermissions(authentication);

        // Then
        verify(rolePermissionRepository).findByKeycloakRoleIn(Set.of(UserRoleEnum.ADMIN));
    }

    @Test
    @DisplayName("Should handle mixed ROLE_ prefix authorities")
    void shouldHandleMixedRolePrefixAuthorities() {
        // Given
        when(authentication.getName()).thenReturn(TEST_USER);
        Collection<GrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority(UserRoleEnum.ADMIN.getRoleWithPrefix()),
                        new SimpleGrantedAuthority("user"));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping1 =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_READ);
        RolePermissionMapping mapping2 =
                new RolePermissionMapping(UserRoleEnum.USER, Permission.REPORTS_READ);
        when(rolePermissionRepository.findByKeycloakRoleIn(
                        Set.of(UserRoleEnum.ADMIN, UserRoleEnum.USER)))
                .thenReturn(List.of(mapping1, mapping2));

        // When
        Set<Permission> permissions = userPermissionCache.getUserPermissions(authentication);

        // Then
        assertThat(permissions).hasSize(2).contains(Permission.USERS_READ, Permission.REPORTS_READ);
        verify(rolePermissionRepository)
                .findByKeycloakRoleIn(Set.of(UserRoleEnum.ADMIN, UserRoleEnum.USER));
    }

    @Test
    @DisplayName("Should cache results per username")
    void shouldCacheResultsPerUsername() {
        // Given
        when(authentication.getName()).thenReturn(TEST_USER);
        Collection<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(UserRoleEnum.ADMIN.getRoleWithPrefix()));
        when(authentication.getAuthorities()).thenAnswer(_ -> authorities);

        RolePermissionMapping mapping =
                new RolePermissionMapping(UserRoleEnum.ADMIN, Permission.USERS_READ);
        when(rolePermissionRepository.findByKeycloakRoleIn(Set.of(UserRoleEnum.ADMIN)))
                .thenReturn(List.of(mapping));

        Set<Permission> permissions1 = userPermissionCache.getUserPermissions(authentication);
        Set<Permission> permissions2 = userPermissionCache.getUserPermissions(authentication);

        assertThat(permissions1).isEqualTo(permissions2).contains(Permission.USERS_READ);
    }
}
