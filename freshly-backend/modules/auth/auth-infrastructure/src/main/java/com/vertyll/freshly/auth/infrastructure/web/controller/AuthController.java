package com.vertyll.freshly.auth.infrastructure.web.controller;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.vertyll.freshly.auth.application.port.inbound.command.SessionUseCase;
import com.vertyll.freshly.auth.domain.model.AuthSession;
import com.vertyll.freshly.auth.infrastructure.config.AuthProperties;
import com.vertyll.freshly.auth.infrastructure.config.KeycloakProperties;
import com.vertyll.freshly.auth.infrastructure.web.dto.SessionResponseDto;
import com.vertyll.freshly.auth.infrastructure.web.session.BrowserSessions;
import com.vertyll.freshly.auth.infrastructure.web.session.Pkce;
import com.vertyll.freshly.auth.infrastructure.web.session.SignInTransaction;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.web.security.PublicEndpoint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private static final String SCOPE = "openid profile email";
    private static final String ERROR_PARAM = "error";
    private static final String SIGN_IN_FAILED = "sign_in_failed";
    private static final String STATE_MISMATCH = "state_mismatch";
    private static final Set<String> ALLOWED_ACTIONS = Set.of("CONFIGURE_TOTP", "UPDATE_PASSWORD", "delete_credential");
    private static final Set<String> UI_LOCALES = Set.of("pl", "en");

    private final SessionUseCase sessions;
    private final BrowserSessions browserSessions;
    private final KeycloakProperties keycloak;
    private final AuthProperties auth;

    @GetMapping("/authorize")
    @PublicEndpoint("the start of sign-in: there is no token yet, and this only builds the Keycloak redirect")
    public ResponseEntity<Void> authorize(
        HttpServletRequest request,
        Locale locale,
        @RequestParam(name = "kc_action", required = false) @Nullable String kcAction,
        @RequestParam(defaultValue = "false") boolean register
    ) {
        SignInTransaction transaction = browserSessions.begin(request);

        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(keycloak.endpoint("auth"))
            .queryParam("client_id", keycloak.userClientId())
            .queryParam("redirect_uri", auth.callbackUrl())
            .queryParam("response_type", "code")
            .queryParam("scope", SCOPE)
            .queryParam("state", transaction.state())
            .queryParam("code_challenge", Pkce.challengeOf(transaction.codeVerifier()))
            .queryParam("code_challenge_method", Pkce.CHALLENGE_METHOD);
        if (UI_LOCALES.contains(locale.getLanguage())) {
            uri.queryParam("ui_locales", locale.getLanguage());
        }
        if (kcAction != null && ALLOWED_ACTIONS.contains(kcAction)) {
            uri.queryParam("kc_action", kcAction);
        }
        if (register) {
            uri.queryParam("prompt", "create");
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(uri.encode().build().toUri()).build();
    }

    @GetMapping("/callback")
    @PublicEndpoint("Keycloak redirects the browser back here with a code; the code, not a token, is the credential")
    public ResponseEntity<Void> callback(
        HttpServletRequest request,
        @RequestParam(required = false) @Nullable String code,
        @RequestParam(required = false) @Nullable String state,
        @RequestParam(name = ERROR_PARAM, required = false) @Nullable String error
    ) {
        Optional<SignInTransaction> transaction = browserSessions.takeTransaction(request);

        if (error != null) {
            log.debug("Keycloak returned an authorization error: {}", error);
            return redirectToApp(SIGN_IN_FAILED);
        }
        if (code == null || state == null || transaction.isEmpty()
                || !Pkce.sameState(transaction.get().state(), state)) {
            log.warn("Rejecting a sign-in callback whose state was not issued to this browser");
            return redirectToApp(STATE_MISMATCH);
        }

        try {
            AuthSession session = sessions.signIn(code, transaction.get().codeVerifier());
            browserSessions.establish(request, session);
            return redirectToApp(null);
        } catch (DomainException e) {
            log.warn("Sign-in could not be completed: {}", e.error());
            return redirectToApp(SIGN_IN_FAILED);
        }
    }

    @GetMapping("/session")
    @PublicEndpoint("answers 204 to a browser without a session, so it cannot require one")
    public ResponseEntity<SessionResponseDto> session(HttpServletRequest request) {
        return browserSessions.current(request)
            .map(session -> ResponseEntity.ok(SessionResponseDto.from(session)))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/logout")
    @PublicEndpoint("ends the session the browser holds, which an expired access token must not prevent")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        browserSessions.current(request).ifPresent(sessions::signOut);
        browserSessions.end(request);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Void> redirectToApp(@Nullable String errorCode) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(auth.postLoginUrl());
        if (errorCode != null) {
            uri.queryParam(ERROR_PARAM, errorCode);
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(uri.build().toUri()).build();
    }
}
