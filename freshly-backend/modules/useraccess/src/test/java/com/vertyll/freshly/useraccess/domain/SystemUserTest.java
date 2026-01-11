package com.vertyll.freshly.useraccess.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.useraccess.domain.exception.*;

class SystemUserTest {

    @Test
    @DisplayName("Should create user with valid data")
    void shouldCreateUserWithValidData() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        boolean isActive = true;
        Set<UserRoleEnum> roles = Set.of(UserRoleEnum.USER);

        // When
        SystemUser user = new SystemUser(keycloakUserId, isActive, roles);

        // Then
        assertThat(user.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getRoles()).containsExactly(UserRoleEnum.USER);
    }

    @Test
    @DisplayName("Should create user with multiple roles")
    void shouldCreateUserWithMultipleRoles() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        Set<UserRoleEnum> roles = Set.of(UserRoleEnum.USER, UserRoleEnum.ADMIN);

        // When
        SystemUser user = new SystemUser(keycloakUserId, true, roles);

        // Then
        assertThat(user.getRoles())
                .containsExactlyInAnyOrder(UserRoleEnum.USER, UserRoleEnum.ADMIN);
    }

    @Test
    @DisplayName("Should throw exception when keycloak user id is null")
    @SuppressWarnings("NullAway")
    void shouldThrowExceptionWhenKeycloakUserIdIsNull() {
        // Given
        Set<UserRoleEnum> roles = Set.of(UserRoleEnum.USER);

        // When & Then
        assertThatThrownBy(() -> new SystemUser(null, true, roles))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Keycloak user ID cannot be null");
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
                .hasMessageContaining("Roles cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when roles are empty")
    void shouldThrowExceptionWhenRolesAreEmpty() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        Set<UserRoleEnum> emptyRoles = Set.of();

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
        Set<UserRoleEnum> roles = Set.of(UserRoleEnum.ADMIN);

        // When
        SystemUser user = SystemUser.reconstitute(keycloakUserId, isActive, roles);

        // Then
        assertThat(user.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(user.isActive()).isFalse();
        assertThat(user.getRoles()).containsExactly(UserRoleEnum.ADMIN);
    }

    @Test
    @DisplayName("Should activate inactive user")
    void shouldActivateInactiveUser() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        Set<UserRoleEnum> roles = Set.of(UserRoleEnum.USER);
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
        SystemUser user = new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER));

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
        SystemUser user = new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER));

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
        SystemUser user = new SystemUser(keycloakUserId, false, Set.of(UserRoleEnum.USER));

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
        SystemUser user = new SystemUser(userId, true, Set.of(UserRoleEnum.USER));

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
        SystemUser user = new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER));

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
        SystemUser user = new SystemUser(keycloakUserId, false, Set.of(UserRoleEnum.USER));

        // When & Then
        assertThatThrownBy(user::deactivateSelf).isInstanceOf(UserAlreadyInactiveException.class);
    }

    @Test
    @DisplayName("Should replace user roles")
    void shouldReplaceUserRoles() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        SystemUser user = new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER));
        Set<UserRoleEnum> newRoles = Set.of(UserRoleEnum.ADMIN);

        // When
        user.replaceRoles(newRoles);

        // Then
        assertThat(user.getRoles()).containsExactly(UserRoleEnum.ADMIN);
    }

    @Test
    @DisplayName("Should replace single role with multiple roles")
    void shouldReplaceSingleRoleWithMultipleRoles() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        SystemUser user = new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER));
        Set<UserRoleEnum> newRoles = Set.of(UserRoleEnum.USER, UserRoleEnum.ADMIN);

        // When
        user.replaceRoles(newRoles);

        // Then
        assertThat(user.getRoles())
                .containsExactlyInAnyOrder(UserRoleEnum.USER, UserRoleEnum.ADMIN);
    }

    @Test
    @DisplayName("Should throw exception when replacing roles with null")
    @SuppressWarnings("NullAway")
    void shouldThrowExceptionWhenReplacingRolesWithNull() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        SystemUser user = new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER));

        // When & Then
        assertThatThrownBy(() -> user.replaceRoles(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Roles cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when replacing roles with empty set")
    void shouldThrowExceptionWhenReplacingRolesWithEmptySet() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        SystemUser user = new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER));
        Set<UserRoleEnum> emptyRoles = Set.of();

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
        Set<UserRoleEnum> roles = Set.of(UserRoleEnum.USER);

        // When
        SystemUser user = new SystemUser(keycloakUserId, isActive, roles);

        // Then
        assertThat(user.isActive()).isFalse();
    }
}
