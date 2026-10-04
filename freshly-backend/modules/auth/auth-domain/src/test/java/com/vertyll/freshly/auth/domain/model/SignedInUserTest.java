package com.vertyll.freshly.auth.domain.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignedInUserTest {
    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("keeps its own copy of the roles")
    void copiesRoles() {
        Set<String> roles = new HashSet<>(Set.of("USER"));
        SignedInUser user = new SignedInUser(SUBJECT, "ada@freshly.local", roles);

        roles.add("ADMIN");

        assertThat(user.roles()).containsExactly("USER");
        assertThatThrownBy(() -> user.roles().add("ADMIN")).isInstanceOf(UnsupportedOperationException.class);
    }
}
