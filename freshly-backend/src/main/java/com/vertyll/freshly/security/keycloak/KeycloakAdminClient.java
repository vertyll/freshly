package com.vertyll.freshly.security.keycloak;

import com.vertyll.freshly.auth.domain.exception.EmailAlreadyExistsException;
import com.vertyll.freshly.auth.domain.exception.InvalidPasswordException;
import com.vertyll.freshly.auth.domain.exception.UsernameAlreadyExistsException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminClient {

    private final Keycloak keycloak;
    private final KeycloakProperties properties;
    private final RestClient restClient;

    private RealmResource getRealm() {
        return keycloak.realm(properties.getRealm());
    }

    public UUID createUser(String username, String email, String password, String firstName, String lastName) {
        UsersResource usersResource = getRealm().users();

        if (!usersResource.searchByUsername(username, true).isEmpty()) {
            throw new UsernameAlreadyExistsException(username);
        }

        if (!usersResource.searchByEmail(email, true).isEmpty()) {
            throw new EmailAlreadyExistsException(email);
        }

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(false); // Disabled until email verified
        user.setEmailVerified(false);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        try (Response response = usersResource.create(user)) {
            if (response.getStatus() != 201) {
                log.error(
                        "Failed to create user in Keycloak: status={}, info={}",
                        response.getStatus(),
                        response.getStatusInfo()
                );
                throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusInfo());
            }

            String locationHeader = response.getHeaderString("Location");
            if (locationHeader == null) {
                throw new RuntimeException("Location header missing from Keycloak response");
            }

            String userId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
            log.info("Created user in Keycloak: username={}, id={}", username, userId);
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format in Keycloak response", e);
            throw new RuntimeException("Invalid user ID format from Keycloak", e);
        }
    }

    public void changePassword(UUID userId, String newPassword) {
        UserResource userResource = getRealm().users().get(userId.toString());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);

        userResource.resetPassword(credential);
        log.info("Password changed for user: {}", userId);
    }

    public void changeEmail(UUID userId, String newEmail) {
        UserResource userResource = getRealm().users().get(userId.toString());
        UserRepresentation user = userResource.toRepresentation();

        List<UserRepresentation> existingUsers = getRealm().users().searchByEmail(newEmail, true);
        if (!existingUsers.isEmpty() && !existingUsers.getFirst().getId().equals(userId.toString())) {
            throw new EmailAlreadyExistsException(newEmail);
        }

        user.setEmail(newEmail);
        user.setEmailVerified(false); // Require re-verification
        userResource.update(user);

        log.info("Email changed for user: {} to: {}", userId, newEmail);
    }

    public void activateUser(UUID userId) {
        UserResource userResource = getRealm().users().get(userId.toString());
        UserRepresentation user = userResource.toRepresentation();

        user.setEnabled(true);
        user.setEmailVerified(true);
        userResource.update(user);

        log.info("User activated: {}", userId);
    }

    public void deleteUser(UUID userId) {
        getRealm().users().delete(userId.toString());
        log.info("User deleted from Keycloak: {}", userId);
    }

    public UserRepresentation getUser(UUID userId) {
        return getRealm().users().get(userId.toString()).toRepresentation();
    }

    public String getUsernameById(UUID userId) {
        UserRepresentation user = getUser(userId);
        return user.getUsername();
    }

    public void verifyPassword(String username, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", properties.getUserClientId());
        formData.add("username", username);
        formData.add("password", password);

        String tokenUrl = properties.getServerUrl() + "/realms/" + properties.getRealm()
                + "/protocol/openid-connect/token";

        try {
            Map<String, Object> response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || !response.containsKey("access_token")) {
                throw new InvalidPasswordException("Current password is incorrect");
            }

            log.debug("Password verification successful for user: {}", username);

        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Password verification failed for user: {}", username);
            throw new InvalidPasswordException("Current password is incorrect");
        } catch (Exception e) {
            log.error("Error verifying password for user: {}", username, e);
            throw new RuntimeException("Failed to verify password", e);
        }
    }

    public List<UserRepresentation> findUsersByEmail(String email) {
        return getRealm().users().searchByEmail(email, true);
    }
}
