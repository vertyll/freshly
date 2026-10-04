package com.vertyll.freshly.auth.infrastructure.keycloak;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.oauth2.jwt.Jwt;

import com.vertyll.freshly.auth.domain.model.SignedInUser;

public final class KeycloakIdentities {
    private static final String EMAIL_CLAIM = "email";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_KEY = "roles";
    private static final String DEFAULT_ROLES_PREFIX = "default-roles-";
    private static final Set<String> BUILT_IN_ROLES = Set.of("offline_access", "uma_authorization");

    private KeycloakIdentities() {
    }

    public static SignedInUser from(Jwt accessToken) {
        String subject = accessToken.getSubject();
        String email = accessToken.getClaimAsString(EMAIL_CLAIM);
        if (subject == null || email == null) {
            throw new IllegalArgumentException("The access token has no subject or no email");
        }
        return new SignedInUser(UUID.fromString(subject), email, roles(accessToken));
    }

    private static Set<String> roles(Jwt accessToken) {
        if (!(accessToken.getClaims().get(REALM_ACCESS_CLAIM) instanceof Map<?, ?> realmAccess)
                || !(realmAccess.get(ROLES_KEY) instanceof Collection<?> roles)) {
            return Set.of();
        }
        return roles.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter(role -> !BUILT_IN_ROLES.contains(role) && !role.startsWith(DEFAULT_ROLES_PREFIX))
            .collect(Collectors.toUnmodifiableSet());
    }
}
