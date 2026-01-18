package com.vertyll.freshly.useraccess.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.useraccess.domain.SystemUser;
import com.vertyll.freshly.useraccess.domain.SystemUserRepository;
import com.vertyll.freshly.useraccess.domain.exception.UserAlreadyExistsException;
import com.vertyll.freshly.useraccess.domain.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserAccessServiceTest {

    private static final long VERSION = 5L;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private SystemUserRepository systemUserRepository;

    @Captor
    @SuppressWarnings("NullAway.Init")
    private ArgumentCaptor<SystemUser> userCaptor;

    private UserAccessService userAccessService;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        userAccessService = new UserAccessService(systemUserRepository);
    }

    @Test
    @DisplayName("Should create user with valid data")
    void shouldCreateUserWithValidData() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        boolean isActive = true;
        Set<String> roles = Set.of(UserRoleEnum.USER.getValue());

        when(systemUserRepository.findById(keycloakUserId)).thenReturn(Optional.empty());
        SystemUser savedUser = new SystemUser(keycloakUserId, isActive, roles);
        when(systemUserRepository.save(any(SystemUser.class))).thenReturn(savedUser);

        // When
        SystemUser result = userAccessService.createUser(keycloakUserId, isActive, roles);

        // Then
        assertThat(result.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getRoles()).containsExactly(UserRoleEnum.USER.getValue());
        verify(systemUserRepository).findById(keycloakUserId);
        verify(systemUserRepository).save(any(SystemUser.class));
    }

    @Test
    @DisplayName("Should throw exception when creating user that already exists")
    void shouldThrowExceptionWhenCreatingUserThatAlreadyExists() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        Set<String> roles = Set.of(UserRoleEnum.USER.getValue());
        SystemUser existingUser = new SystemUser(keycloakUserId, true, roles);

        when(systemUserRepository.findById(keycloakUserId)).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userAccessService.createUser(keycloakUserId, true, roles))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining(keycloakUserId.toString());

        verify(systemUserRepository).findById(keycloakUserId);
        verify(systemUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create user with multiple roles")
    void shouldCreateUserWithMultipleRoles() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        Set<String> roles = Set.of(UserRoleEnum.USER.getValue(), UserRoleEnum.ADMIN.getValue());

        when(systemUserRepository.findById(keycloakUserId)).thenReturn(Optional.empty());
        SystemUser savedUser = new SystemUser(keycloakUserId, true, roles);
        when(systemUserRepository.save(any(SystemUser.class))).thenReturn(savedUser);

        // When
        SystemUser result = userAccessService.createUser(keycloakUserId, true, roles);

        // Then
        assertThat(result.getRoles())
                .containsExactlyInAnyOrder(
                        UserRoleEnum.USER.getValue(), UserRoleEnum.ADMIN.getValue());
    }

    @Test
    @DisplayName("Should get user by id")
    void shouldGetUserById() {
        // Given
        UUID userId = UUID.randomUUID();
        SystemUser user = new SystemUser(userId, true, Set.of(UserRoleEnum.USER.getValue()));
        when(systemUserRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        SystemUser result = userAccessService.getUserById(userId);

        // Then
        assertThat(result).isEqualTo(user);
        assertThat(result.getKeycloakUserId()).isEqualTo(userId);
        verify(systemUserRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        when(systemUserRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userAccessService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(systemUserRepository).findById(userId);
    }

    @Test
    @DisplayName("Should get all users")
    void shouldGetAllUsers() {
        // Given
        SystemUser user1 =
                new SystemUser(UUID.randomUUID(), true, Set.of(UserRoleEnum.USER.getValue()));
        SystemUser user2 =
                new SystemUser(UUID.randomUUID(), false, Set.of(UserRoleEnum.ADMIN.getValue()));
        List<SystemUser> users = List.of(user1, user2);

        when(systemUserRepository.findAll()).thenReturn(users);

        // When
        List<SystemUser> result = userAccessService.getAllUsers();

        // Then
        assertThat(result).hasSize(2).containsExactly(user1, user2);
        verify(systemUserRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void shouldReturnEmptyListWhenNoUsersExist() {
        // Given
        when(systemUserRepository.findAll()).thenReturn(List.of());

        // When
        List<SystemUser> result = userAccessService.getAllUsers();

        // Then
        assertThat(result).isEmpty();
        verify(systemUserRepository).findAll();
    }

    @Test
    @DisplayName("Should activate user")
    void shouldActivateUser() {
        // Given
        UUID userId = UUID.randomUUID();
        SystemUser user =
                SystemUser.reconstitute(
                        userId, false, Set.of(UserRoleEnum.USER.getValue()), VERSION);

        when(systemUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(systemUserRepository.save(any(SystemUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userAccessService.activateUser(userId);

        // Then
        verify(systemUserRepository).findById(userId);
        verify(systemUserRepository).save(userCaptor.capture());
        SystemUser savedUser = userCaptor.getValue();
        assertThat(savedUser.isActive()).isTrue();
        assertThat(savedUser.getVersion()).isEqualTo(VERSION);
    }

    @Test
    @DisplayName("Should throw exception when activating non-existent user")
    void shouldThrowExceptionWhenActivatingNonExistentUser() {
        // Given
        UUID userId = UUID.randomUUID();
        when(systemUserRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userAccessService.activateUser(userId))
                .isInstanceOf(UserNotFoundException.class);

        verify(systemUserRepository).findById(userId);
        verify(systemUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should deactivate user")
    void shouldDeactivateUser() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID loggedInUserId = UUID.randomUUID();
        SystemUser user =
                SystemUser.reconstitute(
                        userId, true, Set.of(UserRoleEnum.USER.getValue()), VERSION);

        when(systemUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(systemUserRepository.save(any(SystemUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userAccessService.deactivateUser(userId, loggedInUserId);

        // Then
        verify(systemUserRepository).findById(userId);
        verify(systemUserRepository).save(userCaptor.capture());
        SystemUser savedUser = userCaptor.getValue();
        assertThat(savedUser.isActive()).isFalse();
        assertThat(savedUser.getVersion()).isEqualTo(VERSION);
    }

    @Test
    @DisplayName("Should throw exception when deactivating non-existent user")
    void shouldThrowExceptionWhenDeactivatingNonExistentUser() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID loggedInUserId = UUID.randomUUID();
        when(systemUserRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userAccessService.deactivateUser(userId, loggedInUserId))
                .isInstanceOf(UserNotFoundException.class);

        verify(systemUserRepository).findById(userId);
        verify(systemUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should replace user roles")
    void shouldReplaceUserRoles() {
        // Given
        UUID userId = UUID.randomUUID();
        Set<String> newRoles = Set.of(UserRoleEnum.ADMIN.getValue());
        SystemUser user =
                SystemUser.reconstitute(
                        userId, true, Set.of(UserRoleEnum.USER.getValue()), VERSION);

        when(systemUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(systemUserRepository.save(any(SystemUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userAccessService.replaceUserRoles(userId, newRoles);

        // Then
        verify(systemUserRepository).findById(userId);
        verify(systemUserRepository).save(userCaptor.capture());
        SystemUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getRoles()).containsExactly(UserRoleEnum.ADMIN.getValue());
        assertThat(savedUser.getVersion()).isEqualTo(VERSION);
    }

    @Test
    @DisplayName("Should replace user roles with multiple roles")
    void shouldReplaceUserRolesWithMultipleRoles() {
        // Given
        UUID userId = UUID.randomUUID();
        Set<String> newRoles = Set.of(UserRoleEnum.USER.getValue(), UserRoleEnum.ADMIN.getValue());
        SystemUser user =
                SystemUser.reconstitute(
                        userId, true, Set.of(UserRoleEnum.USER.getValue()), VERSION);

        when(systemUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(systemUserRepository.save(any(SystemUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userAccessService.replaceUserRoles(userId, newRoles);

        // Then
        verify(systemUserRepository).save(userCaptor.capture());
        SystemUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getRoles())
                .containsExactlyInAnyOrder(
                        UserRoleEnum.USER.getValue(), UserRoleEnum.ADMIN.getValue());
        assertThat(savedUser.getVersion()).isEqualTo(VERSION);
    }

    @Test
    @DisplayName("Should throw exception when replacing roles for non-existent user")
    void shouldThrowExceptionWhenReplacingRolesForNonExistentUser() {
        // Given
        UUID userId = UUID.randomUUID();
        Set<String> newRoles = Set.of(UserRoleEnum.ADMIN.getValue());
        when(systemUserRepository.findById(userId)).thenReturn(Optional.empty());
        // When & Then
        assertThatThrownBy(() -> userAccessService.replaceUserRoles(userId, newRoles))
                .isInstanceOf(UserNotFoundException.class);

        verify(systemUserRepository).findById(userId);
        verify(systemUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create inactive user")
    void shouldCreateInactiveUser() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        boolean isActive = false;
        Set<String> roles = Set.of(UserRoleEnum.USER.getValue());

        when(systemUserRepository.findById(keycloakUserId)).thenReturn(Optional.empty());
        SystemUser savedUser = new SystemUser(keycloakUserId, isActive, roles);
        when(systemUserRepository.save(any(SystemUser.class))).thenReturn(savedUser);

        // When
        SystemUser result = userAccessService.createUser(keycloakUserId, isActive, roles);

        // Then
        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should persist user state after activation")
    void shouldPersistUserStateAfterActivation() {
        // Given
        UUID userId = UUID.randomUUID();
        SystemUser user =
                SystemUser.reconstitute(
                        userId, false, Set.of(UserRoleEnum.USER.getValue()), VERSION);

        when(systemUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(systemUserRepository.save(any(SystemUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userAccessService.activateUser(userId);

        // Then
        verify(systemUserRepository, times(1)).save(any(SystemUser.class));
    }

    @Test
    @DisplayName("Should persist user state after deactivation")
    void shouldPersistUserStateAfterDeactivation() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID loggedInUserId = UUID.randomUUID();
        SystemUser user =
                SystemUser.reconstitute(
                        userId, true, Set.of(UserRoleEnum.USER.getValue()), VERSION);

        when(systemUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(systemUserRepository.save(any(SystemUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userAccessService.deactivateUser(userId, loggedInUserId);

        // Then
        verify(systemUserRepository, times(1)).save(any(SystemUser.class));
    }

    @Test
    @DisplayName("Should persist user state after role replacement")
    void shouldPersistUserStateAfterRoleReplacement() {
        // Given
        UUID userId = UUID.randomUUID();
        Set<String> newRoles = Set.of(UserRoleEnum.ADMIN.getValue());
        SystemUser user =
                SystemUser.reconstitute(
                        userId, true, Set.of(UserRoleEnum.USER.getValue()), VERSION);

        when(systemUserRepository.findById(userId)).thenReturn(Optional.of(user));
        when(systemUserRepository.save(any(SystemUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userAccessService.replaceUserRoles(userId, newRoles);

        // Then
        verify(systemUserRepository, times(1)).save(any(SystemUser.class));
    }
}
