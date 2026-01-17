package com.vertyll.freshly.permission.application;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.permission.Permission;

/**
 * Service responsible for checking user permissions based on their Keycloak roles. Delegates
 * permission fetching to UserPermissionCache for caching support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserPermissionCache permissionCache;

    private static final String AUTH_NULL_OR_NOT_AUTHENTICATED =
            "Authentication is null or not authenticated";

    /**
     * Check if the authenticated user has a specific permission.
     *
     * @param authentication Spring Security authentication object
     * @param permission Permission enum value
     * @return true if user has the permission
     */
    public boolean hasPermission(Authentication authentication, Permission permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug(AUTH_NULL_OR_NOT_AUTHENTICATED);
            return false;
        }

        Set<Permission> userPermissions = permissionCache.getUserPermissions(authentication);

        boolean hasPermission = userPermissions.contains(permission);

        log.debug("Permission check for '{}': {}", permission.getValue(), hasPermission);
        return hasPermission;
    }

    /**
     * Get all permissions for the authenticated user based on their roles. This method delegates to
     * the cache component.
     *
     * @param authentication Spring Security authentication object
     * @return Set of permissions
     */
    public Set<Permission> getUserPermissions(Authentication authentication) {
        return permissionCache.getUserPermissions(authentication);
    }

    /**
     * Check if user has any of the specified permissions.
     *
     * @param authentication Spring Security authentication object
     * @param permissions Variable number of permission enums to check
     * @return true if user has at least one of the specified permissions
     */
    public boolean hasAnyPermission(Authentication authentication, Permission... permissions) {
        Set<Permission> userPermissions = permissionCache.getUserPermissions(authentication);

        for (Permission permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }
}
