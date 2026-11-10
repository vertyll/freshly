package com.vertyll.freshly.auth.infrastructure.keycloak;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.JwkSetUriJwtDecoderBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.DelegatingOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.RestClientRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import com.vertyll.freshly.auth.infrastructure.config.AuthProperties;
import com.vertyll.freshly.auth.infrastructure.config.KeycloakProperties;

import static java.util.Objects.requireNonNull;

/**
 * Keycloak as the OAuth2 client and the resource server see it.
 *
 * <p>
 * Every call to Keycloak — the code exchange, the refresh, the revocation, and the
 * discovery and key-set requests behind the access-token and ID-token decoders — goes
 * through one {@link HttpClient} with {@code application.keycloak.connect-timeout} and
 * {@code read-timeout}, so a Keycloak that stops answering fails a request instead of
 * holding its thread forever.
 */
@Configuration
public class KeycloakConfig {
    public static final String KEYCLOAK_REST_CLIENT = "keycloakRestClient";
    public static final String REGISTRATION_ID = "keycloak";
    private static final String KEYCLOAK_HTTP_CLIENT = "keycloakHttpClient";
    private static final String KEYCLOAK_REQUEST_FACTORY = "keycloakRequestFactory";

    @Bean(name = KEYCLOAK_HTTP_CLIENT, destroyMethod = "close")
    HttpClient keycloakHttpClient(KeycloakProperties keycloak) {
        return HttpClient.newBuilder().connectTimeout(keycloak.connectTimeout()).build();
    }

    @Bean(KEYCLOAK_REQUEST_FACTORY)
    JdkClientHttpRequestFactory keycloakRequestFactory(
        KeycloakProperties keycloak,
        @Qualifier(KEYCLOAK_HTTP_CLIENT) HttpClient httpClient
    ) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(keycloak.readTimeout());
        return requestFactory;
    }

    @Bean(KEYCLOAK_REST_CLIENT)
    RestClient keycloakRestClient(@Qualifier(KEYCLOAK_REQUEST_FACTORY) JdkClientHttpRequestFactory requestFactory) {
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * The resource server's decoder fetches the issuer's metadata and key set through
     * {@code RestOperations}, the one type its builder accepts.
     */
    @Bean
    JwkSetUriJwtDecoderBuilderCustomizer keycloakKeySetTimeouts(
        @Qualifier(KEYCLOAK_REQUEST_FACTORY) JdkClientHttpRequestFactory requestFactory
    ) {
        RestTemplate restOperations = new RestTemplate(requestFactory);
        return builder -> builder.restOperations(restOperations);
    }

    /**
     * The ID-token decoder of the sign-in, as {@link OidcIdTokenDecoderFactory} builds it —
     * the timestamps and the OIDC claims checked, the claim types converted — with the key
     * set fetched through the timed client, which that factory offers no way to set.
     */
    @Bean
    JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory(
        @Qualifier(KEYCLOAK_REQUEST_FACTORY) JdkClientHttpRequestFactory requestFactory
    ) {
        RestTemplate restOperations = new RestTemplate(requestFactory);
        Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();
        return registration -> decoders.computeIfAbsent(registration.getRegistrationId(), _ -> {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                requireNonNull(registration.getProviderDetails().getJwkSetUri(), "the registration names no key set")
            ).restOperations(restOperations).build();
            decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(),
                    new OidcIdTokenValidator(registration)
                )
            );
            decoder.setClaimSetConverter(OidcIdTokenDecoderFactory.createDefaultClaimTypeConverter());
            return decoder;
        });
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(KeycloakProperties keycloak, AuthProperties auth) {
        return new InMemoryClientRegistrationRepository(
            ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientName("Keycloak")
                .clientId(keycloak.clientId())
                .clientSecret(keycloak.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(auth.callbackUrl())
                .scope(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL)
                .authorizationUri(keycloak.endpoint("auth"))
                .tokenUri(keycloak.endpoint("token"))
                .jwkSetUri(keycloak.endpoint("certs"))
                .issuerUri(keycloak.realmUrl())
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .build()
        );
    }

    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient(
        @Qualifier(KEYCLOAK_REQUEST_FACTORY) JdkClientHttpRequestFactory requestFactory
    ) {
        RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
        client.setRestClient(tokenEndpointClient(requestFactory));
        return client;
    }

    @Bean
    OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> refreshTokenResponseClient(
        @Qualifier(KEYCLOAK_REQUEST_FACTORY) JdkClientHttpRequestFactory requestFactory
    ) {
        RestClientRefreshTokenTokenResponseClient client = new RestClientRefreshTokenTokenResponseClient();
        client.setRestClient(tokenEndpointClient(requestFactory));
        return client;
    }

    /**
     * The token endpoint's client as Spring Security's own default builds it — its form and
     * token-response converters, its OAuth2 error handler — on the timed request factory.
     */
    private static RestClient tokenEndpointClient(JdkClientHttpRequestFactory requestFactory) {
        return RestClient.builder()
            .requestFactory(requestFactory)
            .configureMessageConverters(
                converters -> converters.addCustomConverter(new FormHttpMessageConverter())
                    .addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter())
            )
            .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
            .build();
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
        ClientRegistrationRepository clientRegistrations,
        OAuth2AuthorizedClientRepository authorizedClients,
        OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> refreshTokenResponseClient,
        SharedRefreshes sharedRefreshes
    ) {
        DefaultOAuth2AuthorizedClientManager manager =
                new DefaultOAuth2AuthorizedClientManager(clientRegistrations, authorizedClients);
        manager.setAuthorizedClientProvider(
            new DelegatingOAuth2AuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().authorizationCode().build(),
                new SingleFlightRefreshTokenProvider(
                    OAuth2AuthorizedClientProviderBuilder.builder()
                        .refreshToken(refresh -> refresh.accessTokenResponseClient(refreshTokenResponseClient))
                        .build(),
                    sharedRefreshes,
                    Clock.systemUTC()
                )
            )
        );
        return manager;
    }
}
