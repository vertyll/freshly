package com.vertyll.freshly.auth.infrastructure.keycloak;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.vertyll.freshly.auth.application.port.outbound.IdentityProviderPort;
import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.auth.infrastructure.config.KeycloakProperties;
import com.vertyll.freshly.lang.error.DomainException;

import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.requireNonNull;

@Component
@Slf4j
public class KeycloakIdentityProviderAdapter implements IdentityProviderPort {
    private static final String USER_ID = "userId";

    private final Keycloak keycloak;
    private final RestClient restClient;
    private final KeycloakProperties properties;
    private final String realm;

    public KeycloakIdentityProviderAdapter(
        @Qualifier(KeycloakConfig.AUTH_KEYCLOAK) Keycloak keycloakAdminClient,
        @Qualifier(KeycloakConfig.KEYCLOAK_REST_CLIENT) RestClient keycloakRestClient,
        KeycloakProperties properties
    ) {
        this.keycloak = keycloakAdminClient;
        this.restClient = keycloakRestClient;
        this.properties = properties;
        this.realm = properties.realm();
    }

    @Override
    public UUID createUser(NewIdentity identity) {
        UserRepresentation representation = new UserRepresentation();
        representation.setUsername(identity.username());
        representation.setEmail(identity.email());
        representation.setFirstName(identity.firstName());
        representation.setLastName(identity.lastName());
        representation.setEnabled(false);
        representation.setEmailVerified(false);
        representation.setCredentials(List.of(passwordOf(identity.password())));

        try (Response response = users().create(representation)) {
            int status = response.getStatus();

            if (status == Response.Status.CONFLICT.getStatusCode()) {
                throw new DomainException(
                    findByEmail(identity.email()).isPresent() ? AuthError.EMAIL_ALREADY_EXISTS
                            : AuthError.USERNAME_ALREADY_EXISTS
                );
            }

            if (status != Response.Status.CREATED.getStatusCode()) {
                log.error("Keycloak refused user creation with status {}", status);
                throw new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE);
            }

            return extractCreatedId(response);
        }
    }

    @Override
    public boolean deleteUser(UUID userId) {
        try (Response response = users().delete(userId.toString())) {
            boolean removed = response.getStatus() == Response.Status.NO_CONTENT.getStatusCode();
            if (!removed) {
                log.warn("Keycloak returned {} deleting user {}", response.getStatus(), userId);
            }
            return removed;
        } catch (WebApplicationException | ProcessingException e) {
            log.error("Could not delete Keycloak user {}", userId, e);
            return false;
        }
    }

    @Override
    public void enableUser(UUID userId) {
        UserRepresentation representation = new UserRepresentation();
        representation.setEnabled(true);
        representation.setEmailVerified(true);

        update(userId, representation);
    }

    @Override
    public Optional<IdentityUser> findByEmail(String email) {
        List<UserRepresentation> found = users().searchByEmail(email, true);
        return found.isEmpty() ? Optional.empty() : Optional.of(toIdentityUser(found.getFirst()));
    }

    @Override
    public Optional<IdentityUser> findById(UUID userId) {
        try {
            return Optional.of(toIdentityUser(users().get(userId.toString()).toRepresentation()));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean verifyPassword(String username, String password) {
        return KeycloakPasswordCheck.succeeds(restClient, properties, username, password);
    }

    @Override
    public void changePassword(UUID userId, String newPassword) {
        try {
            users().get(userId.toString()).resetPassword(passwordOf(newPassword));
        } catch (NotFoundException e) {
            throw new DomainException(AuthError.USER_NOT_FOUND, Map.of(USER_ID, userId));
        } catch (BadRequestException e) {
            log.warn("Keycloak refused the new password for user {}", userId, e);
            throw new DomainException(AuthError.WEAK_PASSWORD, Map.of(), e);
        } catch (WebApplicationException | ProcessingException e) {
            log.error("Keycloak password reset failed for user {}", userId, e);
            throw new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE, Map.of(), e);
        }
    }

    @Override
    public void changeEmail(UUID userId, String newEmail) {
        UserRepresentation representation = new UserRepresentation();
        representation.setEmail(newEmail);
        representation.setEmailVerified(false);
        representation.setEnabled(false);

        update(userId, representation);
    }

    private void update(UUID userId, UserRepresentation representation) {
        try {
            users().get(userId.toString()).update(representation);
        } catch (NotFoundException e) {
            throw new DomainException(AuthError.USER_NOT_FOUND, Map.of(USER_ID, userId));
        } catch (WebApplicationException | ProcessingException e) {
            log.error("Keycloak update failed for user {}", userId, e);
            throw new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE, Map.of(), e);
        }
    }

    private UsersResource users() {
        return keycloak.realm(realm).users();
    }

    private static CredentialRepresentation passwordOf(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        return credential;
    }

    private static IdentityUser toIdentityUser(UserRepresentation representation) {
        return new IdentityUser(
            UUID.fromString(representation.getId()),
            representation.getUsername(),
            representation.getEmail(),
            requireNonNull(representation.isEnabled(), "Keycloak returned a user with no enabled flag")
        );
    }

    private static UUID extractCreatedId(Response response) {
        String location = response.getLocation() == null ? null : response.getLocation().getPath();
        if (location == null || !location.contains("/")) {
            log.error("Keycloak created a user but returned no usable Location header");
            throw new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE);
        }
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}
