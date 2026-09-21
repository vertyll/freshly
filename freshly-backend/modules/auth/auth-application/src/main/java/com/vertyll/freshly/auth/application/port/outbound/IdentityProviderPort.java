package com.vertyll.freshly.auth.application.port.outbound;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public interface IdentityProviderPort {
    UUID createUser(NewIdentity identity);

    boolean deleteUser(UUID userId);

    void enableUser(UUID userId);

    Optional<IdentityUser> findByEmail(String email);

    Optional<IdentityUser> findById(UUID userId);

    boolean verifyPassword(String username, String password);

    void changePassword(UUID userId, String newPassword);

    void changeEmail(UUID userId, String newEmail);

    record NewIdentity(String username, String email, String password, String firstName, String lastName) {

        @Override
        public String toString() {
            return "NewIdentity[username=" + username + ", email=" + email + ", password=***, firstName=" + firstName
                    + ", lastName=" + lastName + "]";
        }
    }

    record IdentityUser(UUID id, String username, @Nullable String email, boolean enabled) {
        public IdentityUser {
            requireNonNull(id, "Identity user id cannot be null");
            requireNonNull(username, "Identity user username cannot be null");
        }
    }
}
