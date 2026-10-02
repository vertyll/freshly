package com.vertyll.freshly.auth.application.service.command;

import com.vertyll.freshly.auth.application.port.inbound.command.SessionUseCase;
import com.vertyll.freshly.auth.application.port.outbound.TokenIssuerPort;
import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.auth.domain.model.AuthSession;
import com.vertyll.freshly.lang.logging.UseCaseLogger;

public class SessionService implements SessionUseCase {
    private final TokenIssuerPort tokenIssuer;
    private final UserProvisioningPort provisioning;
    private final UseCaseLogger logger;

    public SessionService(TokenIssuerPort tokenIssuer, UserProvisioningPort provisioning, UseCaseLogger logger) {
        this.tokenIssuer = tokenIssuer;
        this.provisioning = provisioning;
        this.logger = logger;
    }

    @Override
    public AuthSession signIn(String code, String codeVerifier) {
        AuthSession session = tokenIssuer.exchange(code, codeVerifier);
        boolean provisioned = false;
        try {
            provisioning.provision(session.subject(), session.roles());
            provisioned = true;
        } finally {
            if (!provisioned) {
                tokenIssuer.revoke(session.refreshToken());
            }
        }
        logger.info("User {} signed in", session.subject());
        return session;
    }

    @Override
    public AuthSession refresh(AuthSession session) {
        return tokenIssuer.refresh(session.refreshToken());
    }

    @Override
    public void signOut(AuthSession session) {
        tokenIssuer.revoke(session.refreshToken());
        logger.info("User {} signed out", session.subject());
    }
}
