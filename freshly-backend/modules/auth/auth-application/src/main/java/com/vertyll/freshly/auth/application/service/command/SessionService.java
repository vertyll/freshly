package com.vertyll.freshly.auth.application.service.command;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.auth.application.dto.AuthTokens;
import com.vertyll.freshly.auth.application.port.inbound.command.SessionUseCase;
import com.vertyll.freshly.auth.application.port.outbound.TokenIssuerPort;
import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.logging.UseCaseLogger;

public class SessionService implements SessionUseCase {
    private final TokenIssuerPort tokenIssuer;
    private final UseCaseLogger logger;

    public SessionService(TokenIssuerPort tokenIssuer, UseCaseLogger logger) {
        this.tokenIssuer = tokenIssuer;
        this.logger = logger;
    }

    @Override
    public AuthTokens login(String username, String password) {
        return tokenIssuer.issue(username, password);
    }

    @Override
    public AuthTokens refresh(@Nullable String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new DomainException(AuthError.REFRESH_TOKEN_INVALID);
        }
        return tokenIssuer.refresh(refreshToken);
    }

    @Override
    public void logout(@Nullable String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            logger.debug("Logout with no refresh token; nothing to revoke");
            return;
        }
        tokenIssuer.revoke(refreshToken);
    }
}
