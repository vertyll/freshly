package com.vertyll.freshly.auth.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.auth.application.command.ChangeEmailCommand;
import com.vertyll.freshly.auth.application.command.ChangePasswordCommand;
import com.vertyll.freshly.auth.application.command.ResetPasswordCommand;
import com.vertyll.freshly.auth.application.port.outbound.IdentityProviderPort;
import com.vertyll.freshly.auth.application.port.outbound.UserNotificationPort;
import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.auth.application.port.outbound.VerificationLinkFactory;
import com.vertyll.freshly.auth.application.port.outbound.VerificationTokenPort;
import com.vertyll.freshly.auth.application.service.command.CredentialsService;
import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.auth.domain.model.TokenPurpose;
import com.vertyll.freshly.auth.domain.model.VerificationToken;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.RecordingUseCaseLogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialsServiceTest {
    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String EMAIL = "ada@example.com";
    private static final String LANGUAGE = "pl";

    private FakeIdentityProvider identity;
    private FakeNotifications notifications;
    private FakeTokens tokens;
    private RecordingProvisioning provisioning;
    private CredentialsService service;

    @BeforeEach
    void setUp() {
        identity = new FakeIdentityProvider();
        notifications = new FakeNotifications();
        tokens = new FakeTokens();
        provisioning = new RecordingProvisioning();

        service = new CredentialsService(
            identity,
            provisioning,
            notifications,
            tokens,
            new FakeLinks(),
            new RecordingUseCaseLogger()
        );
    }

    @Nested
    @DisplayName("initiatePasswordReset")
    class InitiateReset {
        @Test
        @DisplayName("sends a link when the address has an account")
        void sendsForKnownAddress() {
            identity.register(USER, "ada", EMAIL);

            service.initiatePasswordReset(EMAIL, LANGUAGE);

            assertThat(notifications.resetsSent).containsExactly(EMAIL);
        }

        @Test
        @DisplayName("returns normally for an address nobody holds, and sends nothing")
        void staysSilentForUnknownAddress() {
            assertThatCode(() -> service.initiatePasswordReset("nobody@example.com", LANGUAGE))
                .doesNotThrowAnyException();

            assertThat(notifications.resetsSent).isEmpty();
        }

        @Test
        @DisplayName("the reset token is minted for resetting, not for verifying")
        void mintsAResetToken() {
            identity.register(USER, "ada", EMAIL);

            service.initiatePasswordReset(EMAIL, LANGUAGE);

            assertThat(tokens.issuedPurposes).containsExactly(TokenPurpose.PASSWORD_RESET);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {
        @Test
        @DisplayName("validates the token for the reset purpose specifically")
        void validatesForResetPurpose() {
            tokens.willValidateTo(new VerificationToken(USER, EMAIL, TokenPurpose.PASSWORD_RESET));

            service.resetPassword(new ResetPasswordCommand("token", "new-correct-horse"));

            assertThat(tokens.validatedPurposes).containsExactly(TokenPurpose.PASSWORD_RESET);
        }

        @Test
        @DisplayName("a token issued for the wrong purpose changes nothing")
        void refusesWrongPurpose() {
            tokens.failValidationWith(new DomainException(AuthError.TOKEN_WRONG_PURPOSE));

            assertThatThrownBy(() -> service.resetPassword(new ResetPasswordCommand("t", "new-password")))
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(AuthError.TOKEN_WRONG_PURPOSE);

            assertThat(identity.passwordChanges).isEmpty();
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {
        @Test
        @DisplayName("refuses when the current password does not verify")
        void refusesWrongCurrentPassword() {
            identity.register(USER, "ada", EMAIL);
            identity.passwordVerifies = false;

            assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(USER, "wrong", "new-one")))
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(AuthError.CURRENT_PASSWORD_INCORRECT);

            assertThat(identity.passwordChanges).isEmpty();
        }

        @Test
        @DisplayName("refuses for a user the identity provider does not know")
        void refusesUnknownUser() {
            assertThatThrownBy(() -> service.changePassword(new ChangePasswordCommand(USER, "old", "new")))
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(AuthError.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("changeEmail")
    class ChangeEmail {
        @Test
        @DisplayName("sends verification to the new address, addressed by username")
        void verifiesTheNewAddress() {
            identity.register(USER, "ada", EMAIL);

            service.changeEmail(new ChangeEmailCommand(USER, "new@example.com", LANGUAGE));

            assertThat(notifications.verificationRecipients).containsExactly("new@example.com");
            assertThat(notifications.verificationUsernames).containsExactly("ada");
        }

        @Test
        @DisplayName("the message goes out in the language the caller was using")
        void carriesLanguage() {
            identity.register(USER, "ada", EMAIL);

            service.changeEmail(new ChangeEmailCommand(USER, "new@example.com", LANGUAGE));

            assertThat(notifications.verificationLanguages).containsExactly(LANGUAGE);
        }

        @Test
        @DisplayName("deactivates the account until the new address is verified")
        void deactivatesUntilVerified() {
            identity.register(USER, "ada", EMAIL);

            service.changeEmail(new ChangeEmailCommand(USER, "new@example.com", LANGUAGE));

            assertThat(provisioning.deactivated).containsExactly(USER);
        }
    }

    @SuppressWarnings("java:S1186")
    private static final class FakeIdentityProvider implements IdentityProviderPort {
        private final java.util.Map<UUID, IdentityUser> users = new java.util.HashMap<>();
        private final List<UUID> passwordChanges = new ArrayList<>();
        private boolean passwordVerifies = true;

        void register(UUID id, String username, String email) {
            users.put(id, new IdentityUser(id, username, email, true));
        }

        @Override
        public UUID createUser(NewIdentity identity) {
            return UUID.randomUUID();
        }

        @Override
        public boolean deleteUser(UUID userId) {
            return users.remove(userId) != null;
        }

        @Override
        public void enableUser(UUID userId) {
        }

        @Override
        public Optional<IdentityUser> findByEmail(String email) {
            return users.values().stream().filter(u -> email.equals(u.email())).findFirst();
        }

        @Override
        public Optional<IdentityUser> findById(UUID userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public boolean verifyPassword(String username, String password) {
            return passwordVerifies;
        }

        @Override
        public void changePassword(UUID userId, String newPassword) {
            passwordChanges.add(userId);
        }

        @Override
        public void changeEmail(UUID userId, String newEmail) {
            users.computeIfPresent(userId, (id, u) -> new IdentityUser(id, u.username(), newEmail, false));
        }
    }

    @SuppressWarnings("java:S1186")
    private static final class FakeNotifications implements UserNotificationPort {
        private final List<String> resetsSent = new ArrayList<>();
        private final List<String> verificationRecipients = new ArrayList<>();
        private final List<String> verificationUsernames = new ArrayList<>();
        private final List<String> verificationLanguages = new ArrayList<>();

        @Override
        public void sendEmailVerification(String email, String username, String link, String languageTag) {
            verificationRecipients.add(email);
            verificationUsernames.add(username);
            verificationLanguages.add(languageTag);
        }

        @Override
        public void sendPasswordReset(String email, String username, String resetLink, String languageTag) {
            resetsSent.add(email);
        }

        @Override
        public void sendWelcomeEmail(String email, String username, String languageTag) {
        }
    }

    private static final class FakeTokens implements VerificationTokenPort {
        private final List<TokenPurpose> issuedPurposes = new ArrayList<>();
        private final List<TokenPurpose> validatedPurposes = new ArrayList<>();
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
            issuedPurposes.add(purpose);
            return "token";
        }

        @Override
        public VerificationToken validate(String token, TokenPurpose expectedPurpose) {
            validatedPurposes.add(expectedPurpose);
            if (validationFailure != null) {
                throw validationFailure;
            }
            return validationResult;
        }
    }

    @SuppressWarnings("java:S1186")
    private static final class RecordingProvisioning implements UserProvisioningPort {
        private final List<UUID> deactivated = new ArrayList<>();

        @Override
        public void provision(UUID userId, java.util.Set<String> roles, boolean active) {
        }

        @Override
        public void activate(UUID userId) {
        }

        @Override
        public void deactivate(UUID userId) {
            deactivated.add(userId);
        }

        @Override
        public boolean deprovision(UUID userId) {
            return true;
        }
    }

    private static final class FakeLinks implements VerificationLinkFactory {
        @Override
        public String emailVerificationLink(String token) {
            return "https://example.test/v?t=" + token;
        }

        @Override
        public String passwordResetLink(String token) {
            return "https://example.test/r?t=" + token;
        }
    }
}
