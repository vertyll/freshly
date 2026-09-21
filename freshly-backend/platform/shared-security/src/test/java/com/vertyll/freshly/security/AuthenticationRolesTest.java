package com.vertyll.freshly.security;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.vertyll.freshly.authz.CallerRoles;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationRolesTest {

    @Test
    @DisplayName("no authentication is an anonymous caller")
    void nullIsAnonymous() {
        assertThat(AuthenticationRoles.of(null)).isEqualTo(CallerRoles.anonymous());
    }

    @Test
    @DisplayName("an unauthenticated token carries no roles")
    void unauthenticatedIsAnonymous() {
        TestingAuthenticationToken token = new TestingAuthenticationToken("ada", "secret", List.of());
        token.setAuthenticated(false);

        assertThat(AuthenticationRoles.of(token)).isEqualTo(CallerRoles.anonymous());
    }

    @Test
    @DisplayName("strips Spring's ROLE_ prefix and keeps names that never had it")
    void stripsRolePrefix() {
        TestingAuthenticationToken token = new TestingAuthenticationToken(
            "ada",
            "secret",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("USER"))
        );

        assertThat(AuthenticationRoles.of(token).values()).containsExactlyInAnyOrder("ADMIN", "USER");
    }
}
