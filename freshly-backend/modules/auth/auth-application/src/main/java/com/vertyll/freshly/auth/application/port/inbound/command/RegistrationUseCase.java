package com.vertyll.freshly.auth.application.port.inbound.command;

import java.util.UUID;

import com.vertyll.freshly.auth.application.command.RegisterUserCommand;

public interface RegistrationUseCase {
    UUID register(RegisterUserCommand command);

    void verifyEmail(String token);
}
