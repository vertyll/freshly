package com.vertyll.freshly.permission.application;

import com.vertyll.freshly.permission.Permission;
import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import com.vertyll.freshly.permission.domain.RolePermissionMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for checking user permissions based on their Keycloak roles.
 * Uses caching to optimize performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final RolePermissionMappingRepository rolePermissionRepository;

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

        Set<Permission> userPermissions = getUserPermissions(authentication);

        boolean hasPermission = userPermissions.stream()
                .anyMatch(p -> p.getValue().equals(permissionValue));

        log.debug("Permission check for '{}': {}", permissionValue, hasPermission);
        return hasPermission;
    }

    /**
     * Get all permissions for the authenticated user based on their roles.
     * Results are cached per username for performance.
     *
     * @param authentication Spring Security authentication object
     * @return Set of permissions
     */
    @Cacheable(value = "user-permissions", key = "#authentication.name")
    public Set<Permission> getUserPermissions(Authentication authentication) {
        Set<String> roles = extractRoles(authentication);

        log.debug("Fetching permissions for user '{}' with roles: {}",
                authentication.getName(), roles);

        Set<Permission> permissions = rolePermissionRepository.findByKeycloakRoleIn(roles)
                .stream()
                .map(RolePermissionMapping::getPermission)
                .collect(Collectors.toSet());

        log.debug("User '{}' has permissions: {}", authentication.getName(), permissions);
        return permissions;
    }

    /**
     * Check if user has any of the specified permissions.
     */
    public boolean hasAnyPermission(Authentication authentication, String... permissionValues) {
        Set<Permission> userPermissions = getUserPermissions(authentication);

        for (String permissionValue : permissionValues) {
            if (userPermissions.stream().anyMatch(p -> p.getValue().equals(permissionValue))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has all of the specified permissions.
     */
    public boolean hasAllPermissions(Authentication authentication, String... permissionValues) {
        Set<Permission> userPermissions = getUserPermissions(authentication);

        for (String permissionValue : permissionValues) {
            if (userPermissions.stream().noneMatch(p -> p.getValue().equals(permissionValue))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extract Keycloak roles from Spring Security authentication.
     * Removes "ROLE_" prefix that Spring adds.
     */
    private Set<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_")
                        ? authority.substring(5)
                        : authority)
                .collect(Collectors.toSet());
    }
}
