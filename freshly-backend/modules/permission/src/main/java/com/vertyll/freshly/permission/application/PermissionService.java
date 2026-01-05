package com.vertyll.freshly.permission.application;

import com.vertyll.freshly.permission.Permission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service responsible for checking user permissions based on their Keycloak roles.
 * Delegates permission fetching to UserPermissionCache for caching support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserPermissionCache permissionCache;

    /**
     * Check if the authenticated user has a specific permission.
     *
     * @param authentication  Spring Security authentication object
     * @param permissionValue Permission string value (e.g., "users:create")
     * @return true if user has the permission
     */
    public boolean hasPermission(Authentication authentication, String permissionValue) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Authentication is null or not authenticated");
            return false;
        }

        Set<Permission> userPermissions = permissionCache.getUserPermissions(authentication);

        boolean hasPermission = userPermissions.stream()
                .anyMatch(p -> p.getValue().equals(permissionValue));

        log.debug("Permission check for '{}': {}", permissionValue, hasPermission);
        return hasPermission;
    }

    /**
     * Get all permissions for the authenticated user based on their roles.
     * This method delegates to the cache component.
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
     * @param authentication   Spring Security authentication object
     * @param permissionValues Variable number of permission values to check
     * @return true if user has at least one of the specified permissions
     */
    public boolean hasAnyPermission(Authentication authentication, String... permissionValues) {
        Set<Permission> userPermissions = permissionCache.getUserPermissions(authentication);

        for (String permissionValue : permissionValues) {
            if (userPermissions.stream().anyMatch(p -> p.getValue().equals(permissionValue))) {
                return true;
            }
        }
        return false;
    }
}
