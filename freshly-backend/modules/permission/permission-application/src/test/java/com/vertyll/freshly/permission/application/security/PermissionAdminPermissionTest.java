package com.vertyll.freshly.permission.application.security;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.authz.PermissionScope;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionAdminPermissionTest {
    @Test
    @DisplayName("every enum constant has a matching Values field, and vice versa")
    void enumAndConstantsAgree() throws IllegalAccessException {
        Set<String> fromEnum = Arrays.stream(PermissionAdminPermission.values())
            .map(PermissionAdminPermission::value)
            .collect(Collectors.toSet());

        Set<String> fromConstants = collectConstants();

        assertThat(fromConstants).isEqualTo(fromEnum);
    }

    @Test
    @DisplayName("every permission is global, and names this context")
    void scopeAndContext() {
        assertThat(PermissionAdminPermission.values()).allSatisfy(permission -> {
            assertThat(permission.scope()).isEqualTo(PermissionScope.GLOBAL);
            assertThat(permission.context()).isEqualTo(PermissionAdminPermission.CONTEXT);
        });
    }

    @Test
    @DisplayName("permission values are namespaced, so two contexts cannot collide")
    void valuesAreNamespaced() {
        assertThat(PermissionAdminPermission.values())
            .allSatisfy(permission -> assertThat(permission.value()).startsWith("permissions:"));
    }

    private static Set<String> collectConstants() throws IllegalAccessException {
        Set<String> values = new HashSet<>();
        for (Field field : PermissionAdminPermission.Values.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                values.add((String) field.get(null));
            }
        }
        return values;
    }
}
