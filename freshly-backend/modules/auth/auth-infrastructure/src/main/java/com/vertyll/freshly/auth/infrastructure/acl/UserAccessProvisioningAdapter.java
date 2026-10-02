package com.vertyll.freshly.auth.infrastructure.acl;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.useraccess.application.command.CreateUserCommand;
import com.vertyll.freshly.useraccess.application.port.inbound.command.UserAccessCommandUseCase;
import com.vertyll.freshly.useraccess.application.port.inbound.query.UserAccessQueryUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAccessProvisioningAdapter implements UserProvisioningPort {
    private final UserAccessCommandUseCase commands;
    private final UserAccessQueryUseCase queries;

    @Override
    public void provision(UUID userId, Set<String> roles) {
        if (queries.findUser(userId).isEmpty()) {
            commands.createUser(new CreateUserCommand(userId, true, roles));
        }
    }
}
