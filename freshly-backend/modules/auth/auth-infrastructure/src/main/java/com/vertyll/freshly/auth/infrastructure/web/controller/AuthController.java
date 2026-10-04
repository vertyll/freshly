package com.vertyll.freshly.auth.infrastructure.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vertyll.freshly.auth.application.port.inbound.command.SessionUseCase;
import com.vertyll.freshly.auth.infrastructure.keycloak.KeycloakConfig;
import com.vertyll.freshly.auth.infrastructure.keycloak.KeycloakIdentities;
import com.vertyll.freshly.auth.infrastructure.web.dto.SessionResponseDto;
import com.vertyll.freshly.auth.infrastructure.web.session.FetchMetadata;
import com.vertyll.freshly.web.security.PublicEndpoint;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(AuthController.PATH)
@RequiredArgsConstructor
public class AuthController {
    public static final String PATH = "/auth";

    private final SessionUseCase sessions;
    private final OAuth2AuthorizedClientRepository authorizedClients;

    @GetMapping("/session")
    @PublicEndpoint("answers 204 to a browser without a session, so it cannot require one")
    public ResponseEntity<SessionResponseDto> session(@Nullable Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return ResponseEntity.ok(SessionResponseDto.from(KeycloakIdentities.from(token.getToken())));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    @PublicEndpoint("ends the session the browser holds, which an expired access token must not prevent")
    public ResponseEntity<Void> logout(
        HttpServletRequest request,
        HttpServletResponse response,
        @Nullable Authentication authentication
    ) {
        if (!FetchMetadata.sentFromThisOrigin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (authentication != null) {
            OAuth2AuthorizedClient client =
                    authorizedClients.loadAuthorizedClient(KeycloakConfig.REGISTRATION_ID, authentication, request);
            OAuth2RefreshToken refreshToken = client == null ? null : client.getRefreshToken();
            if (refreshToken != null) {
                sessions.signOut(refreshToken.getTokenValue());
            }
        }
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }
}
