package com.vertyll.freshly.permission.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.vertyll.freshly.common.enums.Permission;
import com.vertyll.freshly.common.enums.UserRoleEnum;

class RolePermissionMappingTest {
    private static final String ID_CANNOT_BE_NULL = "ID cannot be null";
    private static final String KEYCLOAK_ROLE_CANNOT_BE_NULL = "Keycloak role cannot be null";
    private static final String PERMISSION_CANNOT_BE_NULL = "Permission cannot be null";

    @Test
    @DisplayName("Should create mapping with generated UUID")
    void shouldCreateMappingWithGeneratedUuid() {
        // Given
        String role = UserRoleEnum.ADMIN.getValue();
        Permission permission = Permission.USERS_READ;

        // When
        RolePermissionMapping mapping = new RolePermissionMapping(role, permission);

        // Then
        assertThat(mapping.getId()).isNotNull();
        assertThat(mapping.getKeycloakRole()).isEqualTo(role);
        assertThat(mapping.getPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should create mapping with specific UUID")
    void shouldCreateMappingWithSpecificUuid() {
        // Given
        UUID id = UUID.randomUUID();
        String role = UserRoleEnum.ADMIN.getValue();
        Permission permission = Permission.USERS_CREATE;

        // When
        RolePermissionMapping mapping =
                RolePermissionMapping.reconstitute(id, role, permission, null);

        // Then
        assertThat(mapping.getId()).isEqualTo(id);
        assertThat(mapping.getKeycloakRole()).isEqualTo(role);
        assertThat(mapping.getPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should reconstitute mapping from repository")
    void shouldReconstituteMappingFromRepository() {
        // Given
        UUID id = UUID.randomUUID();
        String role = UserRoleEnum.MODERATOR.getValue();
        Permission permission = Permission.REPORTS_READ;

        // When
        RolePermissionMapping mapping =
                RolePermissionMapping.reconstitute(id, role, permission, 1L);

        // Then
        assertThat(mapping.getId()).isEqualTo(id);
        assertThat(mapping.getKeycloakRole()).isEqualTo(role);
        assertThat(mapping.getPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should throw exception when reconstituting with null id")
    @SuppressWarnings("NullAway")
    void shouldThrowExceptionWhenReconstitutingWithNullId() {
        // Given
        String role = UserRoleEnum.ADMIN.getValue();
        Permission permission = Permission.USERS_READ;

        // When & Then
        assertThatThrownBy(() -> RolePermissionMapping.reconstitute(null, role, permission, 1L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(ID_CANNOT_BE_NULL);
    }

    @Test
    @DisplayName("Should throw exception when reconstituting with null keycloak role")
    @SuppressWarnings("NullAway")
    void shouldThrowExceptionWhenReconstitutingWithNullKeycloakRole() {
        // Given
        UUID id = UUID.randomUUID();
        Permission permission = Permission.USERS_READ;

        // When & Then
        assertThatThrownBy(() -> RolePermissionMapping.reconstitute(id, null, permission, 1L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(KEYCLOAK_ROLE_CANNOT_BE_NULL);
    }

    @Test
    @DisplayName("Should throw exception when reconstituting with null permission")
    @SuppressWarnings("NullAway")
    void shouldThrowExceptionWhenReconstitutingWithNullPermission() {
        // Given
        UUID id = UUID.randomUUID();
        String role = UserRoleEnum.ADMIN.getValue();

        // When & Then
        assertThatThrownBy(() -> RolePermissionMapping.reconstitute(id, role, null, 1L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(PERMISSION_CANNOT_BE_NULL);
    }

    @Test
    @DisplayName("Should create mappings with different UUIDs")
    void shouldCreateMappingsWithDifferentUuids() {
        // Given
        String role = UserRoleEnum.ADMIN.getValue();
        Permission permission = Permission.USERS_READ;

        // When
        RolePermissionMapping mapping1 = new RolePermissionMapping(role, permission);
        RolePermissionMapping mapping2 = new RolePermissionMapping(role, permission);

        // Then
        assertThat(mapping1.getId()).isNotEqualTo(mapping2.getId());
    }

    @Test
    @DisplayName("Should be immutable")
    void shouldBeImmutable() {
        // Given
        UUID id = UUID.randomUUID();
        String role = UserRoleEnum.ADMIN.getValue();
        Permission permission = Permission.USERS_READ;

        // When
        RolePermissionMapping mapping =
                RolePermissionMapping.reconstitute(id, role, permission, null);

        // Then
        assertThat(mapping.getId()).isEqualTo(id);
        assertThat(mapping.getKeycloakRole()).isEqualTo(role);
        assertThat(mapping.getPermission()).isEqualTo(permission);
        // Values should not change
    }

    @ParameterizedTest
    @EnumSource(Permission.class)
    @DisplayName("Should handle all permission types")
    void shouldHandleAllPermissionTypes(Permission permission) {
        // Given
        String role = UserRoleEnum.ADMIN.getValue();

        // When
        RolePermissionMapping mapping = new RolePermissionMapping(role, permission);

        // Then
        assertThat(mapping.getPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should preserve role and permission values")
    void shouldPreserveRoleAndPermissionValues() {
        // Given
        String role = UserRoleEnum.MODERATOR.getValue();
        Permission permission = Permission.SETTINGS_MANAGE;

        // When
        RolePermissionMapping mapping = new RolePermissionMapping(role, permission);

        // Then
        assertThat(mapping.getKeycloakRole()).isEqualTo(role);
        assertThat(mapping.getPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should reconstitute mapping with all valid data")
    void shouldReconstituteMappingWithAllValidData() {
        // Given
        UUID id = UUID.randomUUID();
        String role = UserRoleEnum.USER.getValue();
        Permission permission = Permission.AUTH_CHANGE_PASSWORD;

        // When
        RolePermissionMapping mapping =
                RolePermissionMapping.reconstitute(id, role, permission, 1L);

        // Then
        assertThat(mapping.getId()).isEqualTo(id);
        assertThat(mapping.getKeycloakRole()).isEqualTo(role);
        assertThat(mapping.getPermission()).isEqualTo(permission);
    }
}
