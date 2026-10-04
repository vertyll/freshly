package com.vertyll.freshly.auth.infrastructure.web.session;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.security.SecurityChainCustomizer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BrowserSignIn implements SecurityChainCustomizer {
    private static final String CALLBACK_PATH = "/auth/callback";

    private final ClientRegistrationRepository clientRegistrations;
    private final OAuth2AuthorizedClientRepository authorizedClients;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokens;
    private final SignInCompletion completion;
    private final SessionAccessTokens sessionAccessTokens;
    private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    @Override
    public void customize(HttpSecurity http) {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .oauth2Login(
                login -> login.loginPage(HostedSignInRequests.AUTHORIZE_PATH)
                    .authorizationEndpoint(
                        endpoint -> endpoint.authorizationRequestResolver(new HostedSignInRequests(clientRegistrations))
                    )
                    .redirectionEndpoint(endpoint -> endpoint.baseUri(CALLBACK_PATH))
                    .tokenEndpoint(endpoint -> endpoint.accessTokenResponseClient(authorizationCodeTokens))
                    .authorizedClientRepository(authorizedClients)
                    .successHandler(completion)
                    .failureHandler(completion)
            )
            .requestCache(RequestCacheConfigurer::disable)
            .addFilterBefore(
                new SessionAccessTokenFilter(sessionAccessTokens, jwtAuthenticationConverter),
                AnonymousAuthenticationFilter.class
            );
    }
}
