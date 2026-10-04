package com.vertyll.freshly.auth.infrastructure.web.session;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

final class SessionAccessTokenFilter extends OncePerRequestFilter {
    private final SessionAccessTokens accessTokens;
    private final Converter<Jwt, AbstractAuthenticationToken> tokenAuthentication;

    SessionAccessTokenFilter(
        SessionAccessTokens accessTokens,
        Converter<Jwt, AbstractAuthenticationToken> tokenAuthentication
    ) {
        super();
        this.accessTokens = accessTokens;
        this.tokenAuthentication = tokenAuthentication;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken session) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            if (FetchMetadata.sentFromThisOrigin(request)) {
                accessTokens.current(session, request, response)
                    .map(tokenAuthentication::convert)
                    .ifPresent(context::setAuthentication);
            }
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }
}
