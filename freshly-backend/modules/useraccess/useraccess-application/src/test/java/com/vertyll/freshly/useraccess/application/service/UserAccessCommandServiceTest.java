package com.vertyll.freshly.useraccess.application.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.RecordingUseCaseLogger;
import com.vertyll.freshly.useraccess.application.FakeRoleDirectory;
import com.vertyll.freshly.useraccess.application.InMemorySystemUserRepository;
import com.vertyll.freshly.useraccess.application.command.CreateUserCommand;
import com.vertyll.freshly.useraccess.application.command.ReplaceUserRolesCommand;
import com.vertyll.freshly.useraccess.application.dto.UserResponse;
import com.vertyll.freshly.useraccess.application.service.command.UserAccessCommandService;
import com.vertyll.freshly.useraccess.domain.error.UserAccessError;
import com.vertyll.freshly.useraccess.domain.model.SystemUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccessCommandServiceTest {
    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Set<String> ROLES = Set.of("USER");

    private InMemorySystemUserRepository users;
    private RecordingUseCaseLogger logger;
    private UserAccessCommandService service;

    @BeforeEach
    void setUp() {
        users = new InMemorySystemUserRepository();
        logger = new RecordingUseCaseLogger();
        service = new UserAccessCommandService(users, new FakeRoleDirectory(Set.of("USER", "ADMIN")), logger);
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {
        @Test
        @DisplayName("stores the user and returns the response")
        void storesTheUser() {
            UserResponse response = service.createUser(new CreateUserCommand(USER, false, ROLES));

            assertThat(response.keycloakUserId()).isEqualTo(USER);
            assertThat(response.active()).isFalse();
            assertThat(users.findByKeycloakUserId(USER)).isPresent();
        }

        @Test
        @DisplayName("refuses a user that already exists")
        void refusesDuplicate() {
            users.seed(SystemUser.create(USER, true, ROLES));

            CreateUserCommand command = new CreateUserCommand(USER, true, ROLES);

            assertThatThrownBy(() -> service.createUser(command)).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.USER_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("a refused creation writes nothing")
        void refusalWritesNothing() {
            users.seed(SystemUser.create(USER, true, ROLES));
            int before = users.saveCount();

            CreateUserCommand command = new CreateUserCommand(USER, true, ROLES);

            assertThatThrownBy(() -> service.createUser(command)).isInstanceOf(DomainException.class);

            assertThat(users.saveCount()).isEqualTo(before);
        }

        @Test
        @DisplayName("the refusal names the id, so the message can say which user")
        void refusalCarriesTheId() {
            users.seed(SystemUser.create(USER, true, ROLES));

            CreateUserCommand command = new CreateUserCommand(USER, true, ROLES);

            assertThatThrownBy(() -> service.createUser(command))
                .asInstanceOf(InstanceOfAssertFactories.type(DomainException.class))
                .extracting(DomainException::params)
                .isEqualTo(Map.of("userId", USER));
        }
    }

    @Nested
    @DisplayName("optimistic locking")
    class OptimisticLocking {
        @Test
        @DisplayName("a matching version is accepted")
        void matchingVersionPasses() {
            users.seed(SystemUser.reconstitute(USER, false, ROLES, 4L));

            assertThatCode(() -> service.activateUser(USER, 4L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a stale version is refused before the aggregate is touched")
        void staleVersionRefused() {
            users.seed(SystemUser.reconstitute(USER, false, ROLES, 4L));

            assertThatThrownBy(() -> service.activateUser(USER, 3L)).extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.VERSION_MISMATCH);

            assertThat(users.findByKeycloakUserId(USER)).get().extracting(SystemUser::isActive).isEqualTo(false);
        }

        @Test
        @DisplayName("a null expected version means no check was requested")
        void nullVersionSkipsTheCheck() {
            users.seed(SystemUser.reconstitute(USER, false, ROLES, 9L));

            assertThatCode(() -> service.activateUser(USER, null)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("deactivateUser")
    class Deactivate {
        @Test
        @DisplayName("passes the actor through, so the aggregate can refuse self-deactivation")
        void refusesSelfDeactivation() {
            users.seed(SystemUser.create(ADMIN, true, ROLES));

            assertThatThrownBy(() -> service.deactivateUser(ADMIN, ADMIN, null))
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.SELF_DEACTIVATION);
        }

        @Test
        @DisplayName("deactivates somebody else")
        void deactivatesAnother() {
            users.seed(SystemUser.create(USER, true, ROLES));

            service.deactivateUser(USER, ADMIN, null);

            assertThat(users.findByKeycloakUserId(USER)).get().extracting(SystemUser::isActive).isEqualTo(false);
        }

        @Test
        @DisplayName("refuses a user that does not exist")
        void refusesUnknownUser() {
            assertThatThrownBy(() -> service.deactivateUser(USER, ADMIN, null))
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("the no-actor form deactivates the subject, self-deactivation notwithstanding")
        void deactivatesWithoutAnActor() {
            users.seed(SystemUser.create(USER, true, ROLES));

            service.deactivateUser(USER, null);

            assertThat(users.findByKeycloakUserId(USER)).get().extracting(SystemUser::isActive).isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("replaceUserRoles")
    class ReplaceRoles {
        @Test
        @DisplayName("replaces the roles and persists once")
        void replacesRoles() {
            users.seed(SystemUser.create(USER, true, Set.of("USER")));
            int before = users.saveCount();

            UserResponse response =
                    service.replaceUserRoles(new ReplaceUserRolesCommand(USER, Set.of("ADMIN", "USER"), null));

            assertThat(response.roles()).containsExactly("ADMIN", "USER");
            assertThat(users.saveCount()).isEqualTo(before + 1);
        }

        @Test
        @DisplayName("refuses an empty role set, and the stored user is unchanged")
        void refusesEmptyRoles() {
            users.seed(SystemUser.create(USER, true, Set.of("USER")));

            assertThatThrownBy(() -> service.replaceUserRoles(new ReplaceUserRolesCommand(USER, Set.of(), null)))
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(UserAccessError.USER_ROLES_EMPTY);

            assertThat(users.findByKeycloakUserId(USER)).get().extracting(SystemUser::roles).isEqualTo(Set.of("USER"));
        }
    }

    @Test
    @DisplayName("logging goes through the port, so it is assertable rather than console output")
    void logsThroughThePort() {
        service.createUser(new CreateUserCommand(USER, true, ROLES));

        assertThat(logger.messages()).anySatisfy(message -> assertThat(message).contains("created"));
    }
}
