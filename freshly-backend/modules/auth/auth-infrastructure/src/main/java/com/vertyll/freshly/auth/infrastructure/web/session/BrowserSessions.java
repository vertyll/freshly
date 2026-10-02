package com.vertyll.freshly.auth.infrastructure.web.session;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.auth.domain.model.AuthSession;

@Component
public class BrowserSessions {
    private static final String TRANSACTION = BrowserSessions.class.getName() + ".transaction";
    private static final String SESSION = BrowserSessions.class.getName() + ".session";
    private static final String REFRESH_LOCK = BrowserSessions.class.getName() + ".refreshLock";

    public SignInTransaction begin(HttpServletRequest request) {
        SignInTransaction transaction = new SignInTransaction(Pkce.newState(), Pkce.newCodeVerifier());
        request.getSession().setAttribute(TRANSACTION, transaction);
        return transaction;
    }

    public Optional<SignInTransaction> takeTransaction(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(TRANSACTION) instanceof SignInTransaction transaction)) {
            return Optional.empty();
        }
        session.removeAttribute(TRANSACTION);
        return Optional.of(transaction);
    }

    public void establish(HttpServletRequest request, AuthSession authSession) {
        end(request);
        HttpSession session = request.getSession();
        session.setAttribute(REFRESH_LOCK, new ReentrantLock());
        session.setAttribute(SESSION, StoredSession.of(authSession));
    }

    public Optional<AuthSession> current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(SESSION) instanceof StoredSession stored)) {
            return Optional.empty();
        }
        return Optional.of(stored.toAuthSession());
    }

    public void replace(HttpServletRequest request, AuthSession authSession) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(SESSION, StoredSession.of(authSession));
        }
    }

    public Optional<Lock> refreshLock(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(REFRESH_LOCK) instanceof Lock lock)) {
            return Optional.empty();
        }
        return Optional.of(lock);
    }

    public void end(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
