package com.vertyll.freshly.auth.application.port.inbound.command;

import com.vertyll.freshly.auth.domain.model.SignedInUser;

public interface SessionUseCase {
    void signIn(SignedInUser user, String refreshToken);

    void signOut(String refreshToken);
}
