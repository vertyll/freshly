package com.vertyll.freshly.auth.application.port.inbound.command;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.auth.application.dto.AuthTokens;

public interface SessionUseCase {
    AuthTokens login(String username, String password);

    AuthTokens refresh(@Nullable String refreshToken);

    void logout(@Nullable String refreshToken);
}
