package com.vertyll.freshly.permission.application;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.permission.Permission;
import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import com.vertyll.freshly.permission.domain.RolePermissionMappingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Component responsible for caching user permissions. Separated from PermissionService to
 * ensure @Cacheable works correctly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class UserPermissionCache {

    private final RolePermissionMappingRepository rolePermissionRepository;

    /**
     * Get all permissions for the authenticated user based on their roles. Results are cached per
     * username for performance.
     *
     * @param authentication Spring Security authentication object
     * @return Set of permissions
     */
    @Cacheable(value = "user-permissions", key = "#authentication.name")
    public Set<Permission> getUserPermissions(Authentication authentication) {
        Set<String> roles = extractRoles(authentication);

        log.debug(
                "Fetching permissions for user '{}' with roles: {}",
                authentication.getName(),
                roles);

        Set<Permission> permissions =
                rolePermissionRepository.findByKeycloakRoleIn(roles).stream()
                        .map(RolePermissionMapping::getPermission)
                        .collect(Collectors.toSet());

        log.debug("User '{}' has permissions: {}", authentication.getName(), permissions);
        return permissions;
    }

    /**
     * Extract Keycloak roles from Spring Security authentication. Removes "ROLE_" prefix that
     * Spring adds.
     */
    private Set<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(
                        authority ->
                                authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .collect(Collectors.toSet());
    }
}
