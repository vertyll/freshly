package com.vertyll.freshly.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads Keycloak realm roles out of a token.
 *
 * <p>
 * A top-level class rather than a nested one inside {@code SecurityConfig}, so it can be
 * tested without standing up the filter chain. It is the piece most worth testing: a token
 * whose {@code realm_access} claim is shaped unexpectedly silently yields no authorities,
 * and the symptom is every request coming back 403 with nothing in the log.
 *
 * <p>
 * {@code ROLE_} is declared here too. The prefix is Spring Security's convention, not a
 * domain concept, so no module should own it.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_KEY = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get(REALM_ACCESS_CLAIM);
        if (!(realmAccess instanceof Map<?, ?> claims)) {
            return List.of();
        }

        Object roles = claims.get(ROLES_KEY);
        if (!(roles instanceof Collection<?> roleNames)) {
            return List.of();
        }

        // Pattern-matched rather than cast: an unchecked cast on a claim controlled by an
        // external issuer is a ClassCastException waiting for a Keycloak upgrade.
        return roleNames.stream()
            .map(KeycloakRealmRoleConverter::nonBlankString)
            .filter(Objects::nonNull)
            .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
            .map(GrantedAuthority.class::cast)
            .toList();
    }

    @Nullable private static String nonBlankString(Object candidate) {
        return candidate instanceof String value && !value.isBlank() ? value : null;
    }
}
