package com.vertyll.freshly.useraccess.application.port.inbound.command;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.useraccess.application.command.CreateUserCommand;
import com.vertyll.freshly.useraccess.application.command.ReplaceUserRolesCommand;
import com.vertyll.freshly.useraccess.application.dto.UserResponse;

public interface UserAccessCommandUseCase {
    UserResponse createUser(CreateUserCommand command);

    void activateUser(UUID userId, @Nullable Long expectedVersion);

    void deactivateUser(UUID userId, UUID actorId, @Nullable Long expectedVersion);

    void deactivateUser(UUID userId, @Nullable Long expectedVersion);

    UserResponse replaceUserRoles(ReplaceUserRolesCommand command);
}
