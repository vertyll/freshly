package com.vertyll.freshly.useraccess.domain.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.useraccess.domain.error.UserAccessError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemUserTest {
    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Set<String> ROLES = Set.of("USER");

    @Nested
    @DisplayName("construction")
    class Construction {
        @Test
        @DisplayName("refuses a user with no roles")
        void refusesEmptyRoles() {
            assertThatThrownBy(() -> SystemUser.create(USER, true, Set.of())).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.USER_ROLES_EMPTY);
        }

        @Test
        @DisplayName("a newly created user carries no version")
        void createHasNoVersion() {
            assertThat(SystemUser.create(USER, true, ROLES).version()).isNull();
        }

        @Test
        @DisplayName("reconstitute is the only way to carry a version back in")
        void reconstituteCarriesVersion() {
            assertThat(SystemUser.reconstitute(USER, true, ROLES, 3L).version()).isEqualTo(3L);
        }

        @Test
        @DisplayName("the role set is copied, so a later change to the caller's set does not leak in")
        void rolesAreCopied() {
            Set<String> mutable = new HashSet<>(Set.of("USER"));
            SystemUser user = SystemUser.create(USER, true, mutable);

            mutable.add("ADMIN");

            assertThat(user.roles()).containsExactly("USER");
        }
    }

    @Nested
    @DisplayName("activation")
    class Activation {
        @Test
        @DisplayName("refuses to activate a user who is already active")
        void refusesDoubleActivation() {
            SystemUser user = SystemUser.create(USER, true, ROLES);

            assertThatThrownBy(user::activate).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.USER_ALREADY_ACTIVE);
        }

        @Test
        @DisplayName("refuses to deactivate a user who is already inactive")
        void refusesDoubleDeactivation() {
            SystemUser user = SystemUser.create(USER, false, ROLES);

            assertThatThrownBy(() -> user.deactivateBy(OTHER)).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.USER_ALREADY_INACTIVE);
        }

        @Test
        @DisplayName("refuses self-deactivation, and says so specifically")
        void refusesSelfDeactivation() {
            SystemUser user = SystemUser.create(USER, true, ROLES);

            assertThatThrownBy(() -> user.deactivateBy(USER)).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.SELF_DEACTIVATION);
        }

        @Test
        @DisplayName("the self-deactivation check runs before the already-inactive check")
        void selfDeactivationOutranksInactive() {
            SystemUser user = SystemUser.create(USER, true, ROLES);

            assertThatThrownBy(() -> user.deactivateBy(USER)).extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.SELF_DEACTIVATION);
        }

        @Test
        @DisplayName("the no-actor form deactivates, for the application withdrawing an account")
        void deactivateWithoutActorIsAllowed() {
            SystemUser user = SystemUser.create(USER, true, ROLES);

            user.deactivate();

            assertThat(user.isActive()).isFalse();
        }

        @Test
        @DisplayName("the no-actor form still refuses a user who is already inactive")
        void deactivateWithoutActorRefusesDoubleDeactivation() {
            SystemUser user = SystemUser.create(USER, false, ROLES);

            assertThatThrownBy(user::deactivate).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.USER_ALREADY_INACTIVE);
        }
    }

    @Nested
    @DisplayName("roles")
    class Roles {
        @Test
        @DisplayName("refuses to replace roles with an empty set")
        void refusesEmptyReplacement() {
            SystemUser user = SystemUser.create(USER, true, ROLES);

            assertThatThrownBy(() -> user.replaceRoles(Set.of())).extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.USER_ROLES_EMPTY);
        }

        @Test
        @DisplayName("a refused replacement leaves the previous roles intact")
        void refusalIsAtomic() {
            SystemUser user = SystemUser.create(USER, true, Set.of("USER"));

            assertThatThrownBy(() -> user.replaceRoles(Set.of())).isInstanceOf(DomainException.class);

            assertThat(user.roles()).containsExactly("USER");
        }
    }
}
