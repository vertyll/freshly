package com.vertyll.freshly.auth.application.port.inbound.command;

import com.vertyll.freshly.auth.domain.model.AuthSession;

public interface SessionUseCase {
    AuthSession signIn(String code, String codeVerifier);

    AuthSession refresh(AuthSession session);

    void signOut(AuthSession session);
}
