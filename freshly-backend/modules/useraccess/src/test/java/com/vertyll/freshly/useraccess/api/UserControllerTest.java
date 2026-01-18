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

import com.vertyll.freshly.common.enums.UserRoleEnum;
import com.vertyll.freshly.useraccess.api.dto.CreateUserRequestDto;
import com.vertyll.freshly.useraccess.api.dto.UpdateUserRolesRequestDto;
import com.vertyll.freshly.useraccess.api.dto.UserResponseDto;
import com.vertyll.freshly.useraccess.api.mapper.UserDtoMapper;
import com.vertyll.freshly.useraccess.application.UserAccessService;
import com.vertyll.freshly.useraccess.domain.SystemUser;
import com.vertyll.freshly.useraccess.domain.exception.UserAlreadyExistsException;
import com.vertyll.freshly.useraccess.domain.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    private static final String SUCCESS = "Success";
    private static final String SUCCESS_USER_CREATED_MSG_KEY = "success.user.created";
    private static final String SUCCESS_USER_FETCHED_MSG_KEY = "success.user.fetched";
    private static final String SUCCESS_USER_LIST_FETCHED_MSG_KEY = "success.user.listFetched";

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
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID keycloakUserId = UUID.randomUUID();
        CreateUserRequestDto request =
                new CreateUserRequestDto(
                        keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));

        SystemUser user =
                new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));
        UserResponseDto responseDto =
                new UserResponseDto(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));

        when(userAccessService.createUser(
                        keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue())))
                .thenReturn(user);
        when(userDtoMapper.toResponse(user)).thenReturn(responseDto);

        // When
        ResponseEntity<?> response = userController.createUser(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userAccessService)
                .createUser(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));
        verify(userDtoMapper).toResponse(user);
    }

    @Test
    @DisplayName("Should create inactive user")
    void shouldCreateInactiveUser() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID keycloakUserId = UUID.randomUUID();
        CreateUserRequestDto request =
                new CreateUserRequestDto(
                        keycloakUserId, false, Set.of(UserRoleEnum.USER.getValue()));

        SystemUser user =
                new SystemUser(keycloakUserId, false, Set.of(UserRoleEnum.USER.getValue()));
        UserResponseDto responseDto =
                new UserResponseDto(keycloakUserId, false, Set.of(UserRoleEnum.USER.getValue()));

        when(userAccessService.createUser(
                        keycloakUserId, false, Set.of(UserRoleEnum.USER.getValue())))
                .thenReturn(user);
        when(userDtoMapper.toResponse(user)).thenReturn(responseDto);

        // When
        ResponseEntity<?> response = userController.createUser(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userAccessService)
                .createUser(keycloakUserId, false, Set.of(UserRoleEnum.USER.getValue()));
    }

    @Test
    @DisplayName("Should create user with multiple roles")
    void shouldCreateUserWithMultipleRoles() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID keycloakUserId = UUID.randomUUID();
        Set<String> roles = Set.of(UserRoleEnum.USER.getValue(), UserRoleEnum.ADMIN.getValue());
        CreateUserRequestDto request = new CreateUserRequestDto(keycloakUserId, true, roles);

        SystemUser user = new SystemUser(keycloakUserId, true, roles);
        UserResponseDto responseDto =
                new UserResponseDto(
                        keycloakUserId,
                        true,
                        Set.of(UserRoleEnum.USER.getValue(), UserRoleEnum.ADMIN.getValue()));

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
                new CreateUserRequestDto(
                        keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));

        when(userAccessService.createUser(
                        keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue())))
                .thenThrow(new UserAlreadyExistsException(keycloakUserId));

        // When & Then
        assertThatThrownBy(() -> userController.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Should get user by id")
    void shouldGetUserById() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID userId = UUID.randomUUID();
        SystemUser user = new SystemUser(userId, true, Set.of(UserRoleEnum.USER.getValue()));
        UserResponseDto responseDto =
                new UserResponseDto(userId, true, Set.of(UserRoleEnum.USER.getValue()));

        when(userAccessService.getUserById(userId)).thenReturn(user);
        when(userDtoMapper.toResponse(user)).thenReturn(responseDto);

        // When
        ResponseEntity<?> response = userController.getUserById(userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccessService).getUserById(userId);
        verify(userDtoMapper).toResponse(user);
        verify(messageSource).getMessage(eq(SUCCESS_USER_FETCHED_MSG_KEY), any(), any());
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
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        SystemUser user1 =
                new SystemUser(UUID.randomUUID(), true, Set.of(UserRoleEnum.USER.getValue()));
        SystemUser user2 =
                new SystemUser(UUID.randomUUID(), false, Set.of(UserRoleEnum.ADMIN.getValue()));
        List<SystemUser> users = List.of(user1, user2);

        UserResponseDto dto1 =
                new UserResponseDto(
                        user1.getKeycloakUserId(), true, Set.of(UserRoleEnum.USER.getValue()));
        UserResponseDto dto2 =
                new UserResponseDto(
                        user2.getKeycloakUserId(), false, Set.of(UserRoleEnum.ADMIN.getValue()));
        List<UserResponseDto> responseDtos = List.of(dto1, dto2);

        when(userAccessService.getAllUsers()).thenReturn(users);
        when(userDtoMapper.toResponseList(users)).thenReturn(responseDtos);

        // When
        ResponseEntity<?> response = userController.getAllUsers();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userAccessService).getAllUsers();
        verify(userDtoMapper).toResponseList(users);
        verify(messageSource).getMessage(eq(SUCCESS_USER_LIST_FETCHED_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void shouldReturnEmptyListWhenNoUsersExist() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
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
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
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
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
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
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID userId = UUID.randomUUID();
        Set<String> newRoles = Set.of(UserRoleEnum.ADMIN.getValue());
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
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID userId = UUID.randomUUID();
        Set<String> newRoles = Set.of(UserRoleEnum.USER.getValue(), UserRoleEnum.ADMIN.getValue());
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
        Set<String> newRoles = Set.of(UserRoleEnum.ADMIN.getValue());
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
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
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
                new CreateUserRequestDto(
                        keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));

        SystemUser user =
                new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));
        UserResponseDto responseDto =
                new UserResponseDto(keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue()));

        when(userAccessService.createUser(
                        keycloakUserId, true, Set.of(UserRoleEnum.USER.getValue())))
                .thenReturn(user);
        when(userDtoMapper.toResponse(user)).thenReturn(responseDto);

        // When
        userController.createUser(request);

        // Then
        verify(messageSource).getMessage(eq(SUCCESS_USER_CREATED_MSG_KEY), any(), any());
    }

    @Test
    @DisplayName("Should handle admin role assignment")
    void shouldHandleAdminRoleAssignment() {
        // Given
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(SUCCESS);
        UUID keycloakUserId = UUID.randomUUID();
        CreateUserRequestDto request =
                new CreateUserRequestDto(
                        keycloakUserId, true, Set.of(UserRoleEnum.ADMIN.getValue()));

        SystemUser user =
                new SystemUser(keycloakUserId, true, Set.of(UserRoleEnum.ADMIN.getValue()));
        UserResponseDto responseDto =
                new UserResponseDto(keycloakUserId, true, Set.of(UserRoleEnum.ADMIN.getValue()));

        when(userAccessService.createUser(
                        keycloakUserId, true, Set.of(UserRoleEnum.ADMIN.getValue())))
                .thenReturn(user);
        when(userDtoMapper.toResponse(user)).thenReturn(responseDto);

        // When
        ResponseEntity<?> response = userController.createUser(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userAccessService)
                .createUser(keycloakUserId, true, Set.of(UserRoleEnum.ADMIN.getValue()));
        verify(messageSource).getMessage(eq(SUCCESS_USER_CREATED_MSG_KEY), any(), any());
    }
}
