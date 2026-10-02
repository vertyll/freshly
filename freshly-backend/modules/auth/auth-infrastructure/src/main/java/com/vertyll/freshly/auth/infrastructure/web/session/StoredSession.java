package com.vertyll.freshly.auth.infrastructure.web.session;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.vertyll.freshly.auth.domain.model.AuthSession;

record StoredSession(
    UUID subject,
    String email,
    String roles,
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final String ROLE_SEPARATOR = ",";

    static StoredSession of(AuthSession session) {
        return new StoredSession(
            session.subject(),
            session.email(),
            String.join(ROLE_SEPARATOR, session.roles()),
            session.accessToken(),
            session.refreshToken(),
            session.accessTokenExpiresAt()
        );
    }

    AuthSession toAuthSession() {
        return new AuthSession(
            subject,
            email,
            roles.isEmpty() ? Set.of() : Set.of(roles.split(ROLE_SEPARATOR)),
            accessToken,
            refreshToken,
            accessTokenExpiresAt
        );
    }

    @Override
    public String toString() {
        return toAuthSession().toString();
    }
}
