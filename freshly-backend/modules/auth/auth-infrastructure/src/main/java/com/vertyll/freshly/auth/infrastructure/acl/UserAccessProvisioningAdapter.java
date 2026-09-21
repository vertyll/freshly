package com.vertyll.freshly.auth.infrastructure.acl;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.useraccess.application.command.CreateUserCommand;
import com.vertyll.freshly.useraccess.application.port.inbound.command.UserAccessCommandUseCase;
import com.vertyll.freshly.useraccess.application.port.inbound.query.UserAccessQueryUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAccessProvisioningAdapter implements UserProvisioningPort {
    private final UserAccessCommandUseCase commands;
    private final UserAccessQueryUseCase queries;

    @Override
    public void provision(UUID userId, Set<String> roles, boolean active) {
        commands.createUser(new CreateUserCommand(userId, active, roles));
    }

    @Override
    public void activate(UUID userId) {
        commands.activateUser(userId, null);
    }

    @Override
    public void deactivate(UUID userId) {
        commands.deactivateUser(userId, null);
    }

    @Override
    public boolean deprovision(UUID userId) {
        if (queries.findUser(userId).isEmpty()) {
            return true;
        }

        try {
            commands.deactivateUser(userId, null);
            return true;
        } catch (DomainException e) {
            log.error("Could not deprovision {} during registration rollback", userId, e);
            return false;
        }
    }
}
