package com.vertyll.freshly.security.keycloak;

import com.vertyll.freshly.auth.domain.exception.EmailAlreadyExistsException;
import com.vertyll.freshly.auth.domain.exception.UsernameAlreadyExistsException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminClientTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private KeycloakProperties properties;

    @Mock
    private RestClient restClient;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private Response response;

    @Captor
    private ArgumentCaptor<UserRepresentation> userCaptor;

    private KeycloakAdminClient keycloakAdminClient;

    @BeforeEach
    void setUp() {
        lenient().when(properties.realm()).thenReturn("test-realm");
        lenient().when(properties.userClientId()).thenReturn("test-client");
        lenient().when(properties.serverUrl()).thenReturn("http://localhost:8080");
        lenient().when(keycloak.realm("test-realm")).thenReturn(realmResource);
        lenient().when(realmResource.users()).thenReturn(usersResource);

        keycloakAdminClient = new KeycloakAdminClient(keycloak, properties, restClient);
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        // Given
        String username = "testuser";
        String email = "test@example.com";
        String password = "password123";
        String firstName = "John";
        String lastName = "Doe";
        UUID expectedUserId = UUID.randomUUID();

        when(usersResource.searchByUsername(username, true)).thenReturn(Collections.emptyList());
        when(usersResource.searchByEmail(email, true)).thenReturn(Collections.emptyList());
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getHeaderString("Location")).thenReturn("http://localhost:8080/users/" + expectedUserId);

        // When
        UUID userId = keycloakAdminClient.createUser(username, email, password, firstName, lastName);

        // Then
        assertThat(userId).isEqualTo(expectedUserId);
        verify(usersResource).create(userCaptor.capture());

        UserRepresentation createdUser = userCaptor.getValue();
        assertThat(createdUser.getUsername()).isEqualTo(username);
        assertThat(createdUser.getEmail()).isEqualTo(email);
        assertThat(createdUser.getFirstName()).isEqualTo(firstName);
        assertThat(createdUser.getLastName()).isEqualTo(lastName);
        assertThat(createdUser.isEnabled()).isFalse();
        assertThat(createdUser.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("Should throw UsernameAlreadyExistsException when username exists")
    void shouldThrowUsernameAlreadyExistsExceptionWhenUsernameExists() {
        // Given
        String username = "existinguser";
        UserRepresentation existingUser = new UserRepresentation();
        when(usersResource.searchByUsername(username, true)).thenReturn(List.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> keycloakAdminClient.createUser(
                username, "test@example.com", "password", "John", "Doe"
        ))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(usersResource, never()).create(any());
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email exists")
    void shouldThrowEmailAlreadyExistsExceptionWhenEmailExists() {
        // Given
        String email = "existing@example.com";
        UserRepresentation existingUser = new UserRepresentation();
        when(usersResource.searchByUsername(anyString(), anyBoolean())).thenReturn(Collections.emptyList());
        when(usersResource.searchByEmail(email, true)).thenReturn(List.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> keycloakAdminClient.createUser(
                "testuser", email, "password", "John", "Doe"
        ))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(usersResource, never()).create(any());
    }

    @Test
    @DisplayName("Should change password successfully")
    void shouldChangePasswordSuccessfully() {
        // Given
        UUID userId = UUID.randomUUID();
        String newPassword = "newPassword123";

        when(usersResource.get(userId.toString())).thenReturn(userResource);
        doNothing().when(userResource).resetPassword(any(CredentialRepresentation.class));

        // When
        keycloakAdminClient.changePassword(userId, newPassword);

        // Then
        verify(userResource).resetPassword(argThat(credential ->
                CredentialRepresentation.PASSWORD.equals(credential.getType()) &&
                credential.getValue().equals(newPassword) &&
                !credential.isTemporary()
        ));
    }

    @Test
    @DisplayName("Should change email successfully")
    void shouldChangeEmailSuccessfully() {
        // Given
        UUID userId = UUID.randomUUID();
        String newEmail = "newemail@example.com";
        UserRepresentation user = new UserRepresentation();
        user.setId(userId.toString());

        when(usersResource.get(userId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(user);
        when(usersResource.searchByEmail(newEmail, true)).thenReturn(Collections.emptyList());
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // When
        keycloakAdminClient.changeEmail(userId, newEmail);

        // Then
        verify(userResource).update(argThat(updatedUser ->
                updatedUser.getEmail().equals(newEmail) &&
                !updatedUser.isEmailVerified()
        ));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when changing to existing email")
    void shouldThrowEmailAlreadyExistsExceptionWhenChangingToExistingEmail() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        String newEmail = "existing@example.com";

        UserRepresentation currentUser = new UserRepresentation();
        currentUser.setId(userId.toString());

        UserRepresentation existingUser = new UserRepresentation();
        existingUser.setId(otherUserId.toString());

        when(usersResource.get(userId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(currentUser);
        when(usersResource.searchByEmail(newEmail, true)).thenReturn(List.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> keycloakAdminClient.changeEmail(userId, newEmail))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userResource, never()).update(any());
    }

    @Test
    @DisplayName("Should activate user successfully")
    void shouldActivateUserSuccessfully() {
        // Given
        UUID userId = UUID.randomUUID();
        UserRepresentation user = new UserRepresentation();

        when(usersResource.get(userId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(user);
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // When
        keycloakAdminClient.activateUser(userId);

        // Then
        verify(userResource).update(argThat(updatedUser ->
                updatedUser.isEnabled() &&
                updatedUser.isEmailVerified()
        ));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void shouldDeleteUserSuccessfully() {
        // Given
        UUID userId = UUID.randomUUID();
        when(usersResource.delete(userId.toString())).thenReturn(response);

        // When
        keycloakAdminClient.deleteUser(userId);

        // Then
        verify(usersResource).delete(userId.toString());
    }

    @Test
    @DisplayName("Should get user by id")
    void shouldGetUserById() {
        // Given
        UUID userId = UUID.randomUUID();
        UserRepresentation expectedUser = new UserRepresentation();
        expectedUser.setId(userId.toString());
        expectedUser.setUsername("testuser");

        when(usersResource.get(userId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(expectedUser);

        // When
        UserRepresentation user = keycloakAdminClient.getUser(userId);

        // Then
        assertThat(user).isEqualTo(expectedUser);
        assertThat(user.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should get username by id")
    void shouldGetUsernameById() {
        // Given
        UUID userId = UUID.randomUUID();
        UserRepresentation user = new UserRepresentation();
        user.setUsername("testuser");

        when(usersResource.get(userId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(user);

        // When
        String username = keycloakAdminClient.getUsernameById(userId);

        // Then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should find users by email")
    void shouldFindUsersByEmail() {
        // Given
        String email = "test@example.com";
        UserRepresentation user1 = new UserRepresentation();
        user1.setEmail(email);
        UserRepresentation user2 = new UserRepresentation();
        user2.setEmail(email);
        List<UserRepresentation> expectedUsers = List.of(user1, user2);

        when(usersResource.searchByEmail(email, true)).thenReturn(expectedUsers);

        // When
        List<UserRepresentation> users = keycloakAdminClient.findUsersByEmail(email);

        // Then
        assertThat(users)
            .hasSize(2)
            .isEqualTo(expectedUsers);
    }
}
