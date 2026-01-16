package com.vertyll.freshly.useraccess.api;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import com.vertyll.freshly.useraccess.api.dto.CreateUserRequestDto;
import com.vertyll.freshly.useraccess.api.dto.UpdateUserRolesRequestDto;
import com.vertyll.freshly.useraccess.api.dto.UserResponseDto;
import com.vertyll.freshly.useraccess.api.mapper.UserDtoMapper;
import com.vertyll.freshly.useraccess.application.UserAccessService;
import com.vertyll.freshly.useraccess.domain.SystemUser;
import com.vertyll.freshly.useraccess.domain.UserRoleEnum;
import com.vertyll.freshly.useraccess.domain.exception.UserAlreadyExistsException;
import com.vertyll.freshly.useraccess.domain.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    @SuppressWarnings("NullAway.Init")
    private UserAccessService userAccessService;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private UserDtoMapper userDtoMapper;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private MessageSource messageSource;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private Jwt jwt;

    private UserController userController;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        userController = new UserController(userAccessService, userDtoMapper, messageSource);
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        UUID keycloakUserId = UUID.randomUUID();
        CreateUserRequestDto request =
                new CreateUserRequestDto(keycloakUserId, true, Set.of(UserRoleEnum.USER));

        SystemUser user = new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER));
        UserResponseDto responseDto =
                new UserResponseDto(keycloakUserId, true, Set.of("USER"), null);

        when(userAccessService.createUser(keycloakUserId, true, Set.of(UserRoleEnum.USER)))
                .thenReturn(user);
        when(userDtoMapper.toResponse(user)).thenReturn(responseDto);

        // When
        ResponseEntity<?> response = userController.createUser(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userAccessService).createUser(keycloakUserId, true, Set.of(UserRoleEnum.USER));
        verify(userDtoMapper).toResponse(user);
    }

    @Test
    @DisplayName("Should create inactive user")
    void shouldCreateInactiveUser() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        UUID keycloakUserId = UUID.randomUUID();
        CreateUserRequestDto request =
                new CreateUserRequestDto(keycloakUserId, false, Set.of(UserRoleEnum.USER));

        SystemUser user = new SystemUser(keycloakUserId, false, Set.of(UserRoleEnum.USER));
        UserResponseDto responseDto =
                new UserResponseDto(keycloakUserId, false, Set.of("USER"), null);

        when(userAccessService.createUser(keycloakUserId, false, Set.of(UserRoleEnum.USER)))
                .thenReturn(user);
        when(userDtoMapper.toResponse(user)).thenReturn(responseDto);

        // When
        ResponseEntity<?> response = userController.createUser(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userAccessService).createUser(keycloakUserId, false, Set.of(UserRoleEnum.USER));
    }

    @Test
    @DisplayName("Should create user with multiple roles")
    void shouldCreateUserWithMultipleRoles() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        UUID keycloakUserId = UUID.randomUUID();
        Set<UserRoleEnum> roles = Set.of(UserRoleEnum.USER, UserRoleEnum.ADMIN);
        CreateUserRequestDto request = new CreateUserRequestDto(keycloakUserId, true, roles);

        SystemUser user = new SystemUser(keycloakUserId, true, roles);
        UserResponseDto responseDto =
                new UserResponseDto(keycloakUserId, true, Set.of("USER", "ADMIN"), null);

        when(userAccessService.createUser(keycloakUserId, true, roles)).thenReturn(user);
        when(userDtoMapper.toResponse(user)).thenReturn(responseDto);

        // When
        ResponseEntity<?> response = userController.createUser(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("Should throw exception when creating duplicate user")
    void shouldThrowExceptionWhenCreatingDuplicateUser() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        CreateUserRequestDto request =
                new CreateUserRequestDto(keycloakUserId, true, Set.of(UserRoleEnum.USER));

        when(userAccessService.createUser(keycloakUserId, true, Set.of(UserRoleEnum.USER)))
                .thenThrow(new UserAlreadyExistsException(keycloakUserId));

        // When & Then
        assertThatThrownBy(() -> userController.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should get user by id")
    void shouldGetUserById() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        UUID userId = UUID.randomUUID();
        SystemUser user = new SystemUser(userId, true, Set.of(UserRoleEnum.USER));
        UserResponseDto responseDto = new UserResponseDto(userId, true, Set.of("USER"), null);

        when(userAccessService.getUserById(userId)).thenReturn(user);
        when(userDtoMapper.toResponse(user)).thenReturn(responseDto);

        // When
        ResponseEntity<?> response = userController.getUserById(userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccessService).getUserById(userId);
        verify(userDtoMapper).toResponse(user);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userAccessService.getUserById(userId)).thenThrow(new UserNotFoundException(userId));

        // When & Then
        assertThatThrownBy(() -> userController.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should get all users")
    void shouldGetAllUsers() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        SystemUser user1 = new SystemUser(UUID.randomUUID(), true, Set.of(UserRoleEnum.USER));
        SystemUser user2 = new SystemUser(UUID.randomUUID(), false, Set.of(UserRoleEnum.ADMIN));
        List<SystemUser> users = List.of(user1, user2);

        UserResponseDto dto1 =
                new UserResponseDto(user1.getKeycloakUserId(), true, Set.of("USER"), null);
        UserResponseDto dto2 =
                new UserResponseDto(user2.getKeycloakUserId(), false, Set.of("ADMIN"), null);
        List<UserResponseDto> responseDtos = List.of(dto1, dto2);

        when(userAccessService.getAllUsers()).thenReturn(users);
        when(userDtoMapper.toResponseList(users)).thenReturn(responseDtos);

        // When
        ResponseEntity<?> response = userController.getAllUsers();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccessService).getAllUsers();
        verify(userDtoMapper).toResponseList(users);
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void shouldReturnEmptyListWhenNoUsersExist() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        when(userAccessService.getAllUsers()).thenReturn(List.of());
        when(userDtoMapper.toResponseList(List.of())).thenReturn(List.of());

        // When
        ResponseEntity<?> response = userController.getAllUsers();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should activate user")
    void shouldActivateUser() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        UUID userId = UUID.randomUUID();
        doNothing().when(userAccessService).activateUser(userId);

        // When
        ResponseEntity<?> response = userController.activateUser(userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccessService).activateUser(userId);
    }

    @Test
    @DisplayName("Should throw exception when activating non-existent user")
    void shouldThrowExceptionWhenActivatingNonExistentUser() {
        // Given
        UUID userId = UUID.randomUUID();
        doThrow(new UserNotFoundException(userId)).when(userAccessService).activateUser(userId);

        // When & Then
        assertThatThrownBy(() -> userController.activateUser(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should deactivate user")
    void shouldDeactivateUser() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        UUID userId = UUID.randomUUID();
        UUID loggedInUserId = UUID.randomUUID();

        when(jwt.getSubject()).thenReturn(loggedInUserId.toString());
        doNothing().when(userAccessService).deactivateUser(userId, loggedInUserId);

        // When
        ResponseEntity<?> response = userController.deactivateUser(userId, jwt);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccessService).deactivateUser(userId, loggedInUserId);
    }

    @Test
    @DisplayName("Should throw exception when deactivating non-existent user")
    void shouldThrowExceptionWhenDeactivatingNonExistentUser() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID loggedInUserId = UUID.randomUUID();

        when(jwt.getSubject()).thenReturn(loggedInUserId.toString());
        doThrow(new UserNotFoundException(userId))
                .when(userAccessService)
                .deactivateUser(userId, loggedInUserId);

        // When & Then
        assertThatThrownBy(() -> userController.deactivateUser(userId, jwt))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should update user roles")
    void shouldUpdateUserRoles() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        UUID userId = UUID.randomUUID();
        Set<UserRoleEnum> newRoles = Set.of(UserRoleEnum.ADMIN);
        UpdateUserRolesRequestDto request = new UpdateUserRolesRequestDto(newRoles);

        doNothing().when(userAccessService).replaceUserRoles(userId, newRoles);

        // When
        ResponseEntity<?> response = userController.updateUserRoles(userId, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccessService).replaceUserRoles(userId, newRoles);
    }

    @Test
    @DisplayName("Should update user roles with multiple roles")
    void shouldUpdateUserRolesWithMultipleRoles() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        UUID userId = UUID.randomUUID();
        Set<UserRoleEnum> newRoles = Set.of(UserRoleEnum.USER, UserRoleEnum.ADMIN);
        UpdateUserRolesRequestDto request = new UpdateUserRolesRequestDto(newRoles);

        doNothing().when(userAccessService).replaceUserRoles(userId, newRoles);

        // When
        ResponseEntity<?> response = userController.updateUserRoles(userId, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccessService).replaceUserRoles(userId, newRoles);
    }

    @Test
    @DisplayName("Should throw exception when updating roles for non-existent user")
    void shouldThrowExceptionWhenUpdatingRolesForNonExistentUser() {
        // Given
        UUID userId = UUID.randomUUID();
        Set<UserRoleEnum> newRoles = Set.of(UserRoleEnum.ADMIN);
        UpdateUserRolesRequestDto request = new UpdateUserRolesRequestDto(newRoles);

        doThrow(new UserNotFoundException(userId))
                .when(userAccessService)
                .replaceUserRoles(userId, newRoles);

        // When & Then
        assertThatThrownBy(() -> userController.updateUserRoles(userId, request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should extract logged-in user id from JWT")
    void shouldExtractLoggedInUserIdFromJwt() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        UUID userId = UUID.randomUUID();
        UUID loggedInUserId = UUID.randomUUID();

        when(jwt.getSubject()).thenReturn(loggedInUserId.toString());
        doNothing().when(userAccessService).deactivateUser(userId, loggedInUserId);

        // When
        userController.deactivateUser(userId, jwt);

        // Then
        verify(jwt).getSubject();
        verify(userAccessService).deactivateUser(userId, loggedInUserId);
    }

    @Test
    @DisplayName("Should use message source for responses")
    void shouldUseMessageSourceForResponses() {
        // Given
        UUID keycloakUserId = UUID.randomUUID();
        CreateUserRequestDto request =
                new CreateUserRequestDto(keycloakUserId, true, Set.of(UserRoleEnum.USER));

        SystemUser user = new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER));
        UserResponseDto responseDto =
                new UserResponseDto(keycloakUserId, true, Set.of("USER"), null);

        when(userAccessService.createUser(keycloakUserId, true, Set.of(UserRoleEnum.USER)))
                .thenReturn(user);
        when(userDtoMapper.toResponse(user)).thenReturn(responseDto);

        // When
        userController.createUser(request);

        // Then
        verify(messageSource).getMessage(eq("success.user.created"), any(), any());
    }

    @Test
    @DisplayName("Should handle admin role assignment")
    void shouldHandleAdminRoleAssignment() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");
        UUID keycloakUserId = UUID.randomUUID();
        CreateUserRequestDto request =
                new CreateUserRequestDto(keycloakUserId, true, Set.of(UserRoleEnum.ADMIN));

        SystemUser user = new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.ADMIN));
        UserResponseDto responseDto =
                new UserResponseDto(keycloakUserId, true, Set.of("ADMIN"), null);

        when(userAccessService.createUser(keycloakUserId, true, Set.of(UserRoleEnum.ADMIN)))
                .thenReturn(user);
        when(userDtoMapper.toResponse(user)).thenReturn(responseDto);

        // When
        ResponseEntity<?> response = userController.createUser(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userAccessService).createUser(keycloakUserId, true, Set.of(UserRoleEnum.ADMIN));
    }
}
