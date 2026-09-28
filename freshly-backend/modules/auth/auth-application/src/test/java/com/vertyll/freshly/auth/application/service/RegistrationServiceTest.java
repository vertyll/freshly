package com.vertyll.freshly.auth.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.auth.application.command.RegisterUserCommand;
import com.vertyll.freshly.auth.application.port.outbound.IdentityProviderPort;
import com.vertyll.freshly.auth.application.port.outbound.UserNotificationPort;
import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.auth.application.port.outbound.VerificationLinkFactory;
import com.vertyll.freshly.auth.application.port.outbound.VerificationTokenPort;
import com.vertyll.freshly.auth.application.service.command.RegistrationService;
import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.auth.domain.model.TokenPurpose;
import com.vertyll.freshly.auth.domain.model.VerificationToken;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.RecordingUseCaseLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationServiceTest {
    private static final RegisterUserCommand COMMAND =
            new RegisterUserCommand("ada", "ada@example.com", "correct-horse-battery", "Ada", "Lovelace", "pl");

    private FakeIdentityProvider identity;
    private FakeProvisioning provisioning;
    private FakeNotifications notifications;
    private FakeTokens tokens;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        identity = new FakeIdentityProvider();
        provisioning = new FakeProvisioning();
        notifications = new FakeNotifications();
        tokens = new FakeTokens();
        service = new RegistrationService(
            identity,
            provisioning,
            notifications,
            tokens,
            new FakeLinks(),
            new RecordingUseCaseLogger()
        );
    }

    @Nested
    @DisplayName("the happy path")
    class HappyPath {
        @Test
        @DisplayName("creates the identity, provisions the user and sends verification")
        void completesEveryStep() {
            UUID userId = service.register(COMMAND);

            assertThat(identity.created).containsKey(userId);
            assertThat(provisioning.provisioned).containsKey(userId);
            assertThat(notifications.verificationsSent).hasSize(1);
        }

        @Test
        @DisplayName("the account starts inactive")
        void startsInactive() {
            UUID userId = service.register(COMMAND);

            assertThat(provisioning.provisioned.get(userId).active()).isFalse();
        }

        @Test
        @DisplayName("sends the welcome message only after everything else succeeded")
        void welcomesLast() {
            service.register(COMMAND);

            assertThat(notifications.welcomesSent).containsExactly(COMMAND.email());
        }
    }

    @Nested
    @DisplayName("compensation")
    class Compensation {
        @Test
        @DisplayName("a failure before any write leaves nothing behind")
        void identityFailureLeavesNothing() {
            identity.failOnCreate(new DomainException(AuthError.USERNAME_ALREADY_EXISTS));

            assertThatThrownBy(() -> service.register(COMMAND)).extracting(e -> ((DomainException) e).error())
                .isEqualTo(AuthError.USERNAME_ALREADY_EXISTS);

            assertThat(identity.created).isEmpty();
            assertThat(provisioning.provisioned).isEmpty();
        }

        @Test
        @DisplayName("a provisioning failure deletes the identity")
        void provisioningFailureRollsBackIdentity() {
            provisioning.failOnProvision(new IllegalStateException("mongo down"));

            assertThatThrownBy(() -> service.register(COMMAND)).isInstanceOf(IllegalStateException.class);

            assertThat(identity.created).isEmpty();
        }

        @Test
        @DisplayName("a mail failure undoes both the provisioning and the identity")
        void mailFailureUnwindsEverything() {
            notifications.failWith(new DomainException(AuthError.IDENTITY_PROVIDER_UNAVAILABLE));

            assertThatThrownBy(() -> service.register(COMMAND)).isInstanceOf(DomainException.class);

            assertThat(provisioning.provisioned).isEmpty();
            assertThat(identity.created).isEmpty();
        }

        @Test
        @DisplayName("compensations run in reverse order")
        void unwindsInReverse() {
            notifications.failWith(new IllegalStateException("smtp down"));

            assertThatThrownBy(() -> service.register(COMMAND)).isInstanceOf(IllegalStateException.class);

            assertThat(identity.deletionOrder).containsExactly("deprovision", "deleteIdentity");
        }

        @Test
        @DisplayName("no welcome message is sent when registration fails")
        void sendsNoWelcomeOnFailure() {
            notifications.failWith(new IllegalStateException("smtp down"));

            assertThatThrownBy(() -> service.register(COMMAND)).isInstanceOf(IllegalStateException.class);

            assertThat(notifications.welcomesSent).isEmpty();
        }

        @Test
        @DisplayName("a failing compensation does not mask the original failure")
        void compensationFailureDoesNotMask() {
            notifications.failWith(new DomainException(AuthError.EMAIL_ALREADY_EXISTS));
            identity.failOnDelete(new IllegalStateException("keycloak down too"));

            assertThatThrownBy(() -> service.register(COMMAND)).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(AuthError.EMAIL_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {
        @Test
        @DisplayName("enables the identity before activating the record")
        void enablesBeforeActivating() {
            UUID userId = service.register(COMMAND);
            tokens.willValidateTo(new VerificationToken(userId, COMMAND.email(), TokenPurpose.EMAIL_VERIFICATION));

            service.verifyEmail("any-token");

            assertThat(identity.enabled).contains(userId);
            assertThat(provisioning.activated).contains(userId);
        }

        @Test
        @DisplayName("a token issued for a password reset is refused")
        void refusesWrongPurpose() {
            tokens.failValidationWith(new DomainException(AuthError.TOKEN_WRONG_PURPOSE));

            assertThatThrownBy(() -> service.verifyEmail("reset-token")).extracting(e -> ((DomainException) e).error())
                .isEqualTo(AuthError.TOKEN_WRONG_PURPOSE);
        }
    }

    private static final class FakeIdentityProvider implements IdentityProviderPort {
        private final Map<UUID, NewIdentity> created = new HashMap<>();
        private final Set<UUID> enabled = new HashSet<>();
        private final List<String> deletionOrder = new ArrayList<>();
        @Nullable private RuntimeException createFailure;
        @Nullable private RuntimeException deleteFailure;

        void failOnCreate(RuntimeException failure) {
            this.createFailure = failure;
        }

        void failOnDelete(RuntimeException failure) {
            this.deleteFailure = failure;
        }

        @Override
        public UUID createUser(NewIdentity identity) {
            if (createFailure != null) {
                throw createFailure;
            }
            UUID id = UUID.randomUUID();
            created.put(id, identity);
            return id;
        }

        @Override
        public boolean deleteUser(UUID userId) {
            deletionOrder.add("deleteIdentity");
            if (deleteFailure != null) {
                throw deleteFailure;
            }
            return created.remove(userId) != null;
        }

        @Override
        public void enableUser(UUID userId) {
            enabled.add(userId);
        }

        @Override
        public Optional<IdentityUser> findByEmail(String email) {
            return Optional.empty();
        }

        @Override
        public Optional<IdentityUser> findById(UUID userId) {
            return Optional.empty();
        }

        @Override
        public boolean verifyPassword(String username, String password) {
            return true;
        }

        @Override
        public void changePassword(UUID userId, String newPassword) {
        }

        @Override
        public void changeEmail(UUID userId, String newEmail) {
        }
    }

    private final class FakeProvisioning implements UserProvisioningPort {
        private final Map<UUID, Provisioned> provisioned = new HashMap<>();
        private final Set<UUID> activated = new HashSet<>();
        @Nullable private RuntimeException provisionFailure;

        void failOnProvision(RuntimeException failure) {
            this.provisionFailure = failure;
        }

        @Override
        public void provision(UUID userId, Set<String> roles, boolean active) {
            if (provisionFailure != null) {
                throw provisionFailure;
            }
            provisioned.put(userId, new Provisioned(roles, active));
        }

        @Override
        public void activate(UUID userId) {
            activated.add(userId);
        }

        @Override
        public void deactivate(UUID userId) {
        }

        @Override
        public boolean deprovision(UUID userId) {
            identity.deletionOrder.add("deprovision");
            return provisioned.remove(userId) != null;
        }

        private record Provisioned(Set<String> roles, boolean active) {
        }
    }

    private static final class FakeNotifications implements UserNotificationPort {
        private final List<String> verificationsSent = new ArrayList<>();
        private final List<String> welcomesSent = new ArrayList<>();
        @Nullable private RuntimeException failure;

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void sendEmailVerification(String email, String username, String link, String languageTag) {
            if (failure != null) {
                throw failure;
            }
            verificationsSent.add(email);
        }

        @Override
        public void sendPasswordReset(String email, String username, String resetLink, String languageTag) {
        }

        @Override
        public void sendWelcomeEmail(String email, String username, String languageTag) {
            welcomesSent.add(email);
        }
    }

    private static final class FakeTokens implements VerificationTokenPort {
        private VerificationToken validationResult;
        @Nullable private RuntimeException validationFailure;

        void willValidateTo(VerificationToken token) {
            this.validationResult = token;
        }

        void failValidationWith(RuntimeException failure) {
            this.validationFailure = failure;
        }

        @Override
        public String issue(UUID userId, String email, TokenPurpose purpose) {
            return "token-for-" + userId;
        }

        @Override
        public VerificationToken validate(String token, TokenPurpose expectedPurpose) {
            if (validationFailure != null) {
                throw validationFailure;
            }
            return validationResult;
        }
    }

    private static final class FakeLinks implements VerificationLinkFactory {
        @Override
        public String emailVerificationLink(String token) {
            return "https://example.test/verify-email?token=" + token;
        }

        @Override
        public String passwordResetLink(String token) {
            return "https://example.test/reset-password?token=" + token;
        }
    }
}
