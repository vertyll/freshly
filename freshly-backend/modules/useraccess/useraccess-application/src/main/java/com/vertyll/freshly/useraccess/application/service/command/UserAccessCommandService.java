package com.vertyll.freshly.useraccess.application.service.command;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.lang.concurrency.VersionGuard;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.UseCaseLogger;
import com.vertyll.freshly.useraccess.application.command.CreateUserCommand;
import com.vertyll.freshly.useraccess.application.command.ReplaceUserRolesCommand;
import com.vertyll.freshly.useraccess.application.dto.UserResponse;
import com.vertyll.freshly.useraccess.application.port.inbound.command.UserAccessCommandUseCase;
import com.vertyll.freshly.useraccess.application.port.outbound.RoleDirectoryPort;
import com.vertyll.freshly.useraccess.domain.error.UserAccessError;
import com.vertyll.freshly.useraccess.domain.model.SystemUser;
import com.vertyll.freshly.useraccess.domain.repository.SystemUserRepository;

public class UserAccessCommandService implements UserAccessCommandUseCase {
    private static final String USER_ID = "userId";

    private final SystemUserRepository users;
    private final RoleDirectoryPort directory;
    private final UseCaseLogger logger;

    public UserAccessCommandService(SystemUserRepository users, RoleDirectoryPort directory, UseCaseLogger logger) {
        this.users = users;
        this.directory = directory;
        this.logger = logger;
    }

    @Override
    public UserResponse createUser(CreateUserCommand command) {
        if (users.existsByKeycloakUserId(command.keycloakUserId())) {
            throw new DomainException(UserAccessError.USER_ALREADY_EXISTS, Map.of(USER_ID, command.keycloakUserId()));
        }

        SystemUser created = users.save(SystemUser.create(command.keycloakUserId(), command.active(), command.roles()));

        logger.info("User {} created with roles {}", created.keycloakUserId(), created.roles());
        return UserResponse.from(created);
    }

    @Override
    public void activateUser(UUID userId, @Nullable Long expectedVersion) {
        SystemUser user = require(userId);
        guardVersion(user, expectedVersion);

        user.activate();
        users.save(user);

        logger.info("User {} activated", userId);
    }

    @Override
    public void deactivateUser(UUID userId, UUID actorId, @Nullable Long expectedVersion) {
        SystemUser user = require(userId);
        guardVersion(user, expectedVersion);

        user.deactivateBy(actorId);
        users.save(user);

        logger.info("User {} deactivated by {}", userId, actorId);
    }

    @Override
    public void deactivateUser(UUID userId, @Nullable Long expectedVersion) {
        SystemUser user = require(userId);
        guardVersion(user, expectedVersion);

        user.deactivate();
        users.save(user);

        logger.info("User {} deactivated by the application", userId);
    }

    @Override
    public UserResponse replaceUserRoles(ReplaceUserRolesCommand command) {
        SystemUser user = require(command.userId());
        guardVersion(user, command.expectedVersion());

        user.replaceRoles(command.roles());
        requireKnownRoles(command.roles());

        directory.replaceRoles(command.userId(), command.roles());
        SystemUser saved = users.save(user);

        logger.info("Roles for user {} replaced with {}", command.userId(), command.roles());
        return UserResponse.from(saved);
    }

    private void requireKnownRoles(Set<String> roles) {
        Set<String> available = directory.availableRoles();
        Set<String> unknown =
                roles.stream().filter(role -> !available.contains(role)).collect(Collectors.toCollection(TreeSet::new));

        if (!unknown.isEmpty()) {
            throw new DomainException(UserAccessError.UNKNOWN_ROLES, Map.of("roles", unknown));
        }
    }

    private SystemUser require(UUID userId) {
        return users.findByKeycloakUserId(userId)
            .orElseThrow(() -> new DomainException(UserAccessError.USER_NOT_FOUND, Map.of(USER_ID, userId)));
    }

    private void guardVersion(SystemUser user, @Nullable Long expectedVersion) {
        VersionGuard.requireMatch(
            user.version(),
            expectedVersion,
            () -> new DomainException(UserAccessError.VERSION_MISMATCH, Map.of(USER_ID, user.keycloakUserId()))
        );
    }
}
