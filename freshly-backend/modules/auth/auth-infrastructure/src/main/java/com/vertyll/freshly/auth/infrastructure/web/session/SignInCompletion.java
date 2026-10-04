package com.vertyll.freshly.auth.infrastructure.web.session;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.vertyll.freshly.auth.application.port.inbound.command.SessionUseCase;
import com.vertyll.freshly.auth.domain.model.SignedInUser;
import com.vertyll.freshly.auth.infrastructure.config.AuthProperties;
import com.vertyll.freshly.auth.infrastructure.keycloak.KeycloakIdentities;
import com.vertyll.freshly.lang.error.DomainException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(
    value = "UNVALIDATED_REDIRECT",
    justification = "Every redirect goes to application.auth.post-login-url from configuration, never to request input"
)
public class SignInCompletion implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    private static final String SIGN_IN_FAILED = "sign_in_failed";
    private static final String STATE_MISMATCH = "state_mismatch";

    private final OAuth2AuthorizedClientRepository authorizedClients;
    private final JwtDecoder accessTokens;
    private final SessionUseCase sessions;
    private final AuthProperties auth;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken signIn)) {
            fail(request, response, authentication, "Not an OAuth2 sign-in");
            return;
        }
        OAuth2AuthorizedClient client =
                authorizedClients.loadAuthorizedClient(signIn.getAuthorizedClientRegistrationId(), signIn, request);
        OAuth2RefreshToken refreshToken = client == null ? null : client.getRefreshToken();
        if (client == null || refreshToken == null) {
            fail(request, response, authentication, "Keycloak issued no refresh token");
            return;
        }
        SignedInUser user;
        try {
            user = KeycloakIdentities.from(accessTokens.decode(client.getAccessToken().getTokenValue()));
        } catch (JwtException | IllegalArgumentException e) {
            sessions.signOut(refreshToken.getTokenValue());
            fail(request, response, authentication, e.getMessage());
            return;
        }
        try {
            sessions.signIn(user, refreshToken.getTokenValue());
        } catch (DomainException | DataAccessException e) {
            fail(request, response, authentication, e.getMessage());
            return;
        }
        response.sendRedirect(auth.postLoginUrl());
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        String errorCode = exception instanceof OAuth2AuthenticationException oauth2 ? oauth2.getError().getErrorCode()
                : exception.getMessage();
        log.warn("Sign-in could not be completed: {}", errorCode);
        boolean stateMismatch =
                "authorization_request_not_found".equals(errorCode) || "invalid_state_parameter".equals(errorCode);
        redirectWithError(response, stateMismatch ? STATE_MISMATCH : SIGN_IN_FAILED);
    }

    private void fail(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication,
        @Nullable String reason
    ) throws IOException {
        log.warn("Sign-in could not be completed: {}", reason);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        redirectWithError(response, SIGN_IN_FAILED);
    }

    private void redirectWithError(HttpServletResponse response, String errorCode) throws IOException {
        response.sendRedirect(
            UriComponentsBuilder.fromUriString(auth.postLoginUrl()).queryParam("error", errorCode).build().toUriString()
        );
    }
}
