package com.vertyll.freshly.permission.domain;

import com.vertyll.freshly.permission.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class RolePermissionMappingTest {

    @Test
    @DisplayName("Should create mapping with generated UUID")
    void shouldCreateMappingWithGeneratedUuid() {
        // Given
        String role = "admin";
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
        String role = "admin";
        Permission permission = Permission.USERS_CREATE;

        // When
        RolePermissionMapping mapping = new RolePermissionMapping(id, role, permission);

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
        String role = "moderator";
        Permission permission = Permission.REPORTS_READ;

        // When
        RolePermissionMapping mapping = RolePermissionMapping.reconstitute(id, role, permission);

        // Then
        assertThat(mapping.getId()).isEqualTo(id);
        assertThat(mapping.getKeycloakRole()).isEqualTo(role);
        assertThat(mapping.getPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should throw exception when reconstituting with null id")
    void shouldThrowExceptionWhenReconstitutingWithNullId() {
        // Given
        String role = "admin";
        Permission permission = Permission.USERS_READ;

        // When & Then
        assertThatThrownBy(() -> RolePermissionMapping.reconstitute(null, role, permission))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when reconstituting with null keycloak role")
    void shouldThrowExceptionWhenReconstitutingWithNullKeycloakRole() {
        // Given
        UUID id = UUID.randomUUID();
        Permission permission = Permission.USERS_READ;

        // When & Then
        assertThatThrownBy(() -> RolePermissionMapping.reconstitute(id, null, permission))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Keycloak role cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when reconstituting with null permission")
    void shouldThrowExceptionWhenReconstitutingWithNullPermission() {
        // Given
        UUID id = UUID.randomUUID();
        String role = "admin";

        // When & Then
        assertThatThrownBy(() -> RolePermissionMapping.reconstitute(id, role, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Permission cannot be null");
    }

    @Test
    @DisplayName("Should create mappings with different UUIDs")
    void shouldCreateMappingsWithDifferentUuids() {
        // Given
        String role = "admin";
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
        String role = "admin";
        Permission permission = Permission.USERS_READ;

        // When
        RolePermissionMapping mapping = new RolePermissionMapping(id, role, permission);

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
        String role = "admin";

        // When
        RolePermissionMapping mapping = new RolePermissionMapping(role, permission);

        // Then
        assertThat(mapping.getPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should preserve role and permission values")
    void shouldPreserveRoleAndPermissionValues() {
        // Given
        String role = "custom_role";
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
        String role = "test_role";
        Permission permission = Permission.AUTH_CHANGE_PASSWORD;

        // When
        RolePermissionMapping mapping = RolePermissionMapping.reconstitute(id, role, permission);

        // Then
        assertThat(mapping.getId()).isEqualTo(id);
        assertThat(mapping.getKeycloakRole()).isEqualTo(role);
        assertThat(mapping.getPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should create mapping with empty role name")
    void shouldCreateMappingWithEmptyRoleName() {
        // Given
        String emptyRole = "";
        Permission permission = Permission.USERS_READ;

        // When
        RolePermissionMapping mapping = new RolePermissionMapping(emptyRole, permission);

        // Then
        assertThat(mapping.getKeycloakRole()).isEmpty();
        assertThat(mapping.getPermission()).isEqualTo(permission);
    }
}
