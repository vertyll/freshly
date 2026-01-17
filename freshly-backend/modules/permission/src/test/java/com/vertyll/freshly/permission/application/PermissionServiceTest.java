package com.vertyll.freshly.permission.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.vertyll.freshly.common.enums.Permission;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {
    @Mock
    @SuppressWarnings("NullAway.Init")
    private UserPermissionCache permissionCache;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private Authentication authentication;

    private PermissionService permissionService;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        permissionService = new PermissionService(permissionCache);
    }

    @Test
    @DisplayName("Should return true when user has required permission")
    void shouldReturnTrueWhenUserHasRequiredPermission() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        Set<Permission> userPermissions = Set.of(Permission.USERS_READ, Permission.USERS_CREATE);
        when(permissionCache.getUserPermissions(authentication)).thenReturn(userPermissions);

        // When
        boolean hasPermission =
                permissionService.hasPermission(authentication, Permission.USERS_READ);

        // Then
        assertThat(hasPermission).isTrue();
        verify(permissionCache).getUserPermissions(authentication);
    }

    @Test
    @DisplayName("Should return false when user does not have required permission")
    void shouldReturnFalseWhenUserDoesNotHaveRequiredPermission() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        Set<Permission> userPermissions = Set.of(Permission.USERS_READ);
        when(permissionCache.getUserPermissions(authentication)).thenReturn(userPermissions);

        // When
        boolean hasPermission =
                permissionService.hasPermission(authentication, Permission.USERS_DELETE);

        // Then
        assertThat(hasPermission).isFalse();
        verify(permissionCache).getUserPermissions(authentication);
    }

    @Test
    @DisplayName("Should return false when authentication is null")
    @SuppressWarnings("NullAway")
    void shouldReturnFalseWhenAuthenticationIsNull() {
        // When
        boolean hasPermission = permissionService.hasPermission(null, Permission.USERS_READ);

        // Then
        assertThat(hasPermission).isFalse();
        verify(permissionCache, never()).getUserPermissions(any());
    }

    @Test
    @DisplayName("Should return false when authentication is not authenticated")
    void shouldReturnFalseWhenAuthenticationIsNotAuthenticated() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(false);

        // When
        boolean hasPermission =
                permissionService.hasPermission(authentication, Permission.USERS_READ);

        // Then
        assertThat(hasPermission).isFalse();
        verify(permissionCache, never()).getUserPermissions(any());
    }

    @Test
    @DisplayName("Should return user permissions from cache")
    void shouldReturnUserPermissionsFromCache() {
        // Given
        Set<Permission> expectedPermissions =
                Set.of(Permission.USERS_READ, Permission.USERS_CREATE, Permission.USERS_UPDATE);
        when(permissionCache.getUserPermissions(authentication)).thenReturn(expectedPermissions);

        // When
        Set<Permission> permissions = permissionService.getUserPermissions(authentication);

        // Then
        assertThat(permissions).isEqualTo(expectedPermissions).hasSize(3);
        verify(permissionCache).getUserPermissions(authentication);
    }

    @Test
    @DisplayName("Should return true when user has any of the required permissions")
    void shouldReturnTrueWhenUserHasAnyOfRequiredPermissions() {
        // Given
        Set<Permission> userPermissions = Set.of(Permission.USERS_READ, Permission.REPORTS_READ);
        when(permissionCache.getUserPermissions(authentication)).thenReturn(userPermissions);

        // When
        boolean hasAnyPermission =
                permissionService.hasAnyPermission(
                        authentication,
                        Permission.USERS_CREATE,
                        Permission.USERS_READ,
                        Permission.USERS_DELETE);

        // Then
        assertThat(hasAnyPermission).isTrue();
        verify(permissionCache).getUserPermissions(authentication);
    }

    @Test
    @DisplayName("Should return false when user has none of the required permissions")
    void shouldReturnFalseWhenUserHasNoneOfRequiredPermissions() {
        // Given
        Set<Permission> userPermissions = Set.of(Permission.REPORTS_READ);
        when(permissionCache.getUserPermissions(authentication)).thenReturn(userPermissions);

        // When
        boolean hasAnyPermission =
                permissionService.hasAnyPermission(
                        authentication,
                        Permission.USERS_CREATE,
                        Permission.USERS_READ,
                        Permission.USERS_DELETE);

        // Then
        assertThat(hasAnyPermission).isFalse();
        verify(permissionCache).getUserPermissions(authentication);
    }

    @Test
    @DisplayName("Should handle empty permission set")
    void shouldHandleEmptyPermissionSet() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        when(permissionCache.getUserPermissions(authentication)).thenReturn(Set.of());

        // When
        boolean hasPermission =
                permissionService.hasPermission(authentication, Permission.USERS_READ);

        // Then
        assertThat(hasPermission).isFalse();
    }

    @Test
    @DisplayName("Should return false for hasAnyPermission with empty varargs")
    void shouldReturnFalseForHasAnyPermissionWithEmptyVarargs() {
        // Given
        Set<Permission> userPermissions = Set.of(Permission.USERS_READ);
        when(permissionCache.getUserPermissions(authentication)).thenReturn(userPermissions);

        // When
        boolean hasAnyPermission = permissionService.hasAnyPermission(authentication);

        // Then
        assertThat(hasAnyPermission).isFalse();
    }

    @Test
    @DisplayName("Should check permission case-sensitively")
    void shouldCheckPermissionCaseSensitively() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        Set<Permission> userPermissions = Set.of(Permission.USERS_READ);
        when(permissionCache.getUserPermissions(authentication)).thenReturn(userPermissions);

        // When
        boolean hasPermission =
                permissionService.hasPermission(authentication, Permission.USERS_CREATE);

        // Then
        assertThat(hasPermission).isFalse();
    }

    @Test
    @DisplayName("Should return true when user has all required permissions")
    void shouldReturnTrueWhenUserHasAllRequiredPermissions() {
        // Given
        when(authentication.isAuthenticated()).thenReturn(true);
        Set<Permission> userPermissions =
                Set.of(Permission.USERS_READ, Permission.USERS_CREATE, Permission.USERS_UPDATE);
        when(permissionCache.getUserPermissions(authentication)).thenReturn(userPermissions);

        // When
        boolean hasRead = permissionService.hasPermission(authentication, Permission.USERS_READ);
        boolean hasCreate =
                permissionService.hasPermission(authentication, Permission.USERS_CREATE);
        boolean hasUpdate =
                permissionService.hasPermission(authentication, Permission.USERS_UPDATE);

        // Then
        assertThat(hasRead).isTrue();
        assertThat(hasCreate).isTrue();
        assertThat(hasUpdate).isTrue();
    }

    @Test
    @DisplayName("Should return false for hasAnyPermission when authentication is null")
    @SuppressWarnings("NullAway")
    void shouldReturnFalseForHasAnyPermissionWhenAuthenticationIsNull() {
        // When
        when(permissionCache.getUserPermissions(null)).thenReturn(Set.of());
        boolean hasAnyPermission = permissionService.hasAnyPermission(null, Permission.USERS_READ);

        // Then
        assertThat(hasAnyPermission).isFalse();
    }
}
