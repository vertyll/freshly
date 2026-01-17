package com.vertyll.freshly.permission.application;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.permission.Permission;
import com.vertyll.freshly.permission.domain.RolePermissionMapping;
import com.vertyll.freshly.permission.domain.RolePermissionMappingRepository;

/**
 * Component responsible for caching user permissions. Separated from PermissionService to
 * ensure @Cacheable works correctly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class UserPermissionCache {
    private static final String USER_PERMISSIONS_CACHE = "user-permissions";

    private final RolePermissionMappingRepository rolePermissionRepository;

    /**
     * Get all permissions for the authenticated user based on their roles. Results are cached per
     * username for performance.
     *
     * @param authentication Spring Security authentication object
     * @return Set of permissions
     */
    @Cacheable(value = USER_PERMISSIONS_CACHE, key = "#authentication.name")
    public Set<Permission> getUserPermissions(Authentication authentication) {
        Set<UserRoleEnum> roles = extractRoles(authentication);

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
     * Spring adds and normalizes to uppercase using Locale.ROOT for security.
     */
    private Set<UserRoleEnum> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(
                        authority ->
                                authority.startsWith(UserRoleEnum.ROLE_PREFIX)
                                        ? authority.substring(UserRoleEnum.ROLE_PREFIX.length())
                                        : authority)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(UserRoleEnum::fromValue)
                .collect(Collectors.toSet());
    }
}
