package com.vertyll.freshly.auth.infrastructure.web.session;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vertyll.freshly.auth.application.port.inbound.command.SessionUseCase;
import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.auth.domain.model.AuthSession;
import com.vertyll.freshly.lang.error.DomainException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SessionTokenRelayFilter extends OncePerRequestFilter {
    private static final Duration REFRESH_SKEW = Duration.ofSeconds(30);
    private static final String AUTH_PATH = "/auth/";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String FETCH_SITE_HEADER = "Sec-Fetch-Site";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Set<String> TRUSTED_FETCH_SITES = Set.of("same-origin", "none");

    private final BrowserSessions browserSessions;
    private final SessionUseCase sessions;
    private final Clock clock;

    public SessionTokenRelayFilter(BrowserSessions browserSessions, SessionUseCase sessions, Clock clock) {
        super();
        this.browserSessions = browserSessions;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + AUTH_PATH)
                || request.getHeader(HttpHeaders.AUTHORIZATION) != null || !sentBySameOrigin(request);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        AuthSession session = browserSessions.current(request).isPresent() ? freshen(request) : null;
        chain.doFilter(session == null ? request : new BearerRequest(request, session.accessToken()), response);
    }

    private @Nullable AuthSession freshen(HttpServletRequest request) {
        Lock lock = browserSessions.refreshLock(request).orElse(null);
        if (lock == null) {
            return null;
        }
        lock.lock();
        try {
            AuthSession current = browserSessions.current(request).orElse(null);
            if (current == null || !current.needsRefreshAt(clock.instant(), REFRESH_SKEW)) {
                return current;
            }
            return refreshed(request, current);
        } finally {
            lock.unlock();
        }
    }

    private @Nullable AuthSession refreshed(HttpServletRequest request, AuthSession current) {
        try {
            AuthSession refreshed = sessions.refresh(current);
            browserSessions.replace(request, refreshed);
            return refreshed;
        } catch (DomainException e) {
            if (e.error() == AuthError.SESSION_EXPIRED) {
                browserSessions.end(request);
            } else {
                log.warn("Could not refresh the session of {}: {}", current.subject(), e.error());
            }
            return null;
        }
    }

    private static boolean sentBySameOrigin(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod())) {
            return true;
        }
        String fetchSite = request.getHeader(FETCH_SITE_HEADER);
        return fetchSite == null || TRUSTED_FETCH_SITES.contains(fetchSite);
    }

    private static final class BearerRequest extends HttpServletRequestWrapper {
        private final String authorization;

        BearerRequest(HttpServletRequest request, String accessToken) {
            super(request);
            this.authorization = BEARER_PREFIX + accessToken;
        }

        @Override
        public @Nullable String getHeader(String name) {
            return isAuthorization(name) ? authorization : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return isAuthorization(name) ? Collections.enumeration(Set.of(authorization)) : super.getHeaders(name);
        }

        private static boolean isAuthorization(String name) {
            return String.CASE_INSENSITIVE_ORDER.compare(HttpHeaders.AUTHORIZATION, name) == 0;
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
            names.add(HttpHeaders.AUTHORIZATION);
            return Collections.enumeration(names);
        }
    }
}
