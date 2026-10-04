package com.vertyll.freshly.auth.application.service.command;

import com.vertyll.freshly.auth.application.port.inbound.command.SessionUseCase;
import com.vertyll.freshly.auth.application.port.outbound.SessionRevocationPort;
import com.vertyll.freshly.auth.application.port.outbound.UserProvisioningPort;
import com.vertyll.freshly.auth.domain.model.SignedInUser;
import com.vertyll.freshly.lang.logging.UseCaseLogger;

public class SessionService implements SessionUseCase {
    private final SessionRevocationPort revocation;
    private final UserProvisioningPort provisioning;
    private final UseCaseLogger logger;

    public SessionService(SessionRevocationPort revocation, UserProvisioningPort provisioning, UseCaseLogger logger) {
        this.revocation = revocation;
        this.provisioning = provisioning;
        this.logger = logger;
    }

    @Override
    public void signIn(SignedInUser user, String refreshToken) {
        boolean provisioned = false;
        try {
            provisioning.provision(user.subject(), user.roles());
            provisioned = true;
        } finally {
            if (!provisioned) {
                revocation.revoke(refreshToken);
            }
        }
        logger.info("User {} signed in", user.subject());
    }

    @Override
    public void signOut(String refreshToken) {
        revocation.revoke(refreshToken);
        logger.info("A session was signed out");
    }
}
