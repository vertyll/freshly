package com.vertyll.freshly.useraccess.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.useraccess.domain.exception.*;

class SystemUserTest {
    private static final String KEYCLOAK_USER_ID_CANNOT_BE_NULL = "Keycloak user ID cannot be null";
    private static final String ROLES_CANNOT_BE_NULL = "Roles cannot be null";

    @Test
    @DisplayName("Should create user with valid data")
    void shouldCreateUserWithValidData() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        boolean isActive = true;
        Set<String> roles = Set.of(UserRoleEnum.USER.getValue());

        // When
        SystemUser user = new SystemUser(keycloakUserId, isActive, roles);

        // Then
        assertThat(user.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getRoles()).containsExactly(UserRoleEnum.USER.getValue());
    }

    @Test
    @DisplayName("Should create user with multiple roles")
    void shouldCreateUserWithMultipleRoles() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        Set<String> roles = Set.of(UserRoleEnum.USER.getValue(), UserRoleEnum.ADMIN.getValue());

        // When
        SystemUser user = new SystemUser(keycloakUserId, true, roles);

        // Then
        assertThat(user.getRoles())
                .containsExactlyInAnyOrder(
                        UserRoleEnum.USER.getValue(), UserRoleEnum.ADMIN.getValue());
    }

    @Test
    @DisplayName("Should throw exception when keycloak user id is null")
    @SuppressWarnings("NullAway")
    void shouldThrowExceptionWhenKeycloakUserIdIsNull() {
        // Given
        Set<String> roles = Set.of(UserRoleEnum.USER.getValue());

        // When & Then
        assertThatThrownBy(() -> new SystemUser(null, true, roles))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(KEYCLOAK_USER_ID_CANNOT_BE_NULL);
    }

    @Test
    @DisplayName("Should throw exception when roles are null")
    @SuppressWarnings("NullAway")
    void shouldThrowExceptionWhenRolesAreNull() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> new SystemUser(keycloakUserId, true, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(ROLES_CANNOT_BE_NULL);
    }

    @Test
    @DisplayName("Should throw exception when roles are empty")
    void shouldThrowExceptionWhenRolesAreEmpty() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        Set<String> emptyRoles = Set.of();

        // When & Then
        assertThatThrownBy(() -> new SystemUser(keycloakUserId, true, emptyRoles))
                .isInstanceOf(UserRolesEmptyException.class);
    }

    @Test
    @DisplayName("Should reconstitute user from repository")
    void shouldReconstituteUserFromRepository() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        boolean isActive = false;
        Set<String> roles = Set.of(UserRoleEnum.ADMIN.getValue());

        // When
        SystemUser user = SystemUser.reconstitute(keycloakUserId, isActive, roles, 1L);

        // Then
        assertThat(user.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(user.isActive()).isFalse();
        assertThat(user.getRoles()).containsExactly(UserRoleEnum.ADMIN.getValue());
    }

    @Test
    @DisplayName("Should activate inactive user")
    void shouldActivateInactiveUser() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        Set<String> roles = Set.of(UserRoleEnum.USER.getValue());
        SystemUser user = new SystemUser(keycloakUserId, false, roles);

        // When
        user.activate();

        // Then
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when activating already active user")
    void shouldThrowExceptionWhenActivatingAlreadyActiveUser() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        SystemUser user =
                new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));

        // When & Then
        assertThatThrownBy(user::activate)
                .isInstanceOf(UserAlreadyActiveException.class)
                .hasMessageContaining(keycloakUserId.toString());
    }

    @Test
    @DisplayName("Should deactivate active user")
    void shouldDeactivateActiveUser() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        UUID loggedInUserId = UUID.randomUUID();
        SystemUser user =
                new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));

        // When
        user.deactivate(loggedInUserId);

        // Then
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when deactivating already inactive user")
    void shouldThrowExceptionWhenDeactivatingAlreadyInactiveUser() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        UUID loggedInUserId = UUID.randomUUID();
        SystemUser user =
                new SystemUser(keycloakUserId, false, Set.of(UserRoleEnum.USER.getValue()));

        // When & Then
        assertThatThrownBy(() -> user.deactivate(loggedInUserId))
                .isInstanceOf(UserAlreadyInactiveException.class)
                .hasMessageContaining(keycloakUserId.toString());
    }

    @Test
    @DisplayName("Should throw exception when user tries to deactivate themselves")
    void shouldThrowExceptionWhenUserTriesToDeactivateThemselves() {
        // Given
        UUID userId = UUID.randomUUID();
        SystemUser user = new SystemUser(userId, true, Set.of(UserRoleEnum.USER.getValue()));

        // When & Then
        assertThatThrownBy(() -> user.deactivate(userId))
                .isInstanceOf(SelfDeactivationException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    @DisplayName("Should deactivate self")
    void shouldDeactivateSelf() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        SystemUser user =
                new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));

        // When
        user.deactivateSelf();

        // Then
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when deactivating self while already inactive")
    void shouldThrowExceptionWhenDeactivatingSelfWhileAlreadyInactive() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        SystemUser user =
                new SystemUser(keycloakUserId, false, Set.of(UserRoleEnum.USER.getValue()));

        // When & Then
        assertThatThrownBy(user::deactivateSelf).isInstanceOf(UserAlreadyInactiveException.class);
    }

    @Test
    @DisplayName("Should replace user roles")
    void shouldReplaceUserRoles() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        SystemUser user =
                new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));
        Set<String> newRoles = Set.of(UserRoleEnum.ADMIN.getValue());

        // When
        user.replaceRoles(newRoles);

        // Then
        assertThat(user.getRoles()).containsExactly(UserRoleEnum.ADMIN.getValue());
    }

    @Test
    @DisplayName("Should replace single role with multiple roles")
    void shouldReplaceSingleRoleWithMultipleRoles() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        SystemUser user =
                new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));
        Set<String> newRoles = Set.of(UserRoleEnum.USER.getValue(), UserRoleEnum.ADMIN.getValue());

        // When
        user.replaceRoles(newRoles);

        // Then
        assertThat(user.getRoles())
                .containsExactlyInAnyOrder(
                        UserRoleEnum.USER.getValue(), UserRoleEnum.ADMIN.getValue());
    }

    @Test
    @DisplayName("Should throw exception when replacing roles with null")
    @SuppressWarnings("NullAway")
    void shouldThrowExceptionWhenReplacingRolesWithNull() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        SystemUser user =
                new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));

        // When & Then
        assertThatThrownBy(() -> user.replaceRoles(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(ROLES_CANNOT_BE_NULL);
    }

    @Test
    @DisplayName("Should throw exception when replacing roles with empty set")
    void shouldThrowExceptionWhenReplacingRolesWithEmptySet() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        SystemUser user =
                new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));
        Set<String> emptyRoles = Set.of();

        // When & Then
        assertThatThrownBy(() -> user.replaceRoles(emptyRoles))
                .isInstanceOf(UserRolesEmptyException.class);
    }

    @Test
    @DisplayName("Should create inactive user")
    void shouldCreateInactiveUser() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        boolean isActive = false;
        Set<String> roles = Set.of(UserRoleEnum.USER.getValue());

        // When
        SystemUser user = new SystemUser(keycloakUserId, isActive, roles);

        // Then
        assertThat(user.isActive()).isFalse();
    }
}
