package com.vertyll.freshly.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.vertyll.freshly.authz.CallerRoles;

/**
 * The one place a Spring Security {@code Authentication} becomes role names.
 */
public final class AuthenticationRoles {

    private static final String ROLE_PREFIX = "ROLE_";

    private AuthenticationRoles() {
    }

    public static CallerRoles of(@Nullable Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return CallerRoles.anonymous();
        }

        Set<String> roles = authentication.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .map(AuthenticationRoles::stripPrefix)
            .collect(Collectors.toUnmodifiableSet());

        return CallerRoles.of(roles);
    }

    /**
     * Spring prefixes role authorities with {@code ROLE_}; Keycloak's realm roles do not
     * carry it. Stripping here means a grant is stored under the name an administrator
     * typed rather than one only Spring uses.
     *
     * <p>
     * Only the prefix. Case and whitespace are {@code CallerRoles}' invariant, and
     * normalizing in two places is how the two definitions drift apart.
     */
    private static String stripPrefix(String authority) {
        return authority.startsWith(ROLE_PREFIX) ? authority.substring(ROLE_PREFIX.length()) : authority;
    }
}
