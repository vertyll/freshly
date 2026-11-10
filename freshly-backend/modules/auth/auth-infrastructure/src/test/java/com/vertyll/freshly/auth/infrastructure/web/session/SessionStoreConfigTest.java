package com.vertyll.freshly.auth.infrastructure.web.session;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionStoreConfigTest {
    private static final String REGISTRATION_ID = "keycloak";
    private static final Instant ISSUED = Instant.parse("2026-11-10T08:00:00Z");
    private static final Instant EXPIRES = ISSUED.plusSeconds(300);

    private final RedisSerializer<Object> serializer = SessionStoreConfig.sessionSerializer();

    @Test
    @DisplayName("the authorized client, with its tokens, survives a round trip through the store")
    void authorizedClientRoundTrips() {
        Map<String, OAuth2AuthorizedClient> clients = new HashMap<>();
        clients.put(REGISTRATION_ID, authorizedClient());

        Object read = serializer.deserialize(serializer.serialize(clients));

        assertThat(read).isInstanceOf(Map.class);
        OAuth2AuthorizedClient client =
                requireNonNull((OAuth2AuthorizedClient) ((Map<?, ?>) read).get(REGISTRATION_ID));
        assertThat(client.getPrincipalName()).isEqualTo("ada");
        assertThat(client.getClientRegistration().getRegistrationId()).isEqualTo(REGISTRATION_ID);
        assertThat(client.getAccessToken().getTokenValue()).isEqualTo("access-token");
        assertThat(client.getAccessToken().getExpiresAt()).isEqualTo(EXPIRES);
        assertThat(client.getAccessToken().getScopes()).containsExactlyInAnyOrder("openid", "email");
        assertThat(requireNonNull(client.getRefreshToken()).getTokenValue()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("the security context of a sign-in survives a round trip through the store")
    void securityContextRoundTrips() throws MalformedURLException {
        OidcIdToken idToken = new OidcIdToken(
            "id-token",
            ISSUED,
            EXPIRES,
            Map.of("sub", "ada", "iss", URI.create("https://kc").toURL())
        );
        DefaultOidcUser user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken);
        SecurityContextImpl context =
                new SecurityContextImpl(new OAuth2AuthenticationToken(user, user.getAuthorities(), REGISTRATION_ID));

        Object read = serializer.deserialize(serializer.serialize(context));

        assertThat(read).isInstanceOf(SecurityContextImpl.class);
        OAuth2AuthenticationToken authentication =
                (OAuth2AuthenticationToken) requireNonNull(((SecurityContextImpl) read).getAuthentication());
        assertThat(authentication.getName()).isEqualTo("ada");
        assertThat(authentication.getAuthorizedClientRegistrationId()).isEqualTo(REGISTRATION_ID);
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("a stored value naming a class outside the allowed ones is refused")
    void refusesAnUnknownType() {
        byte[] stored = "{\"@class\":\"java.lang.ProcessBuilder\",\"command\":[\"true\"]}".getBytes(UTF_8);

        assertThatThrownBy(() -> serializer.deserialize(stored)).isInstanceOf(SerializationException.class);
    }

    private static OAuth2AuthorizedClient authorizedClient() {
        ClientRegistration registration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
            .clientId("freshly-app-client")
            .clientSecret("secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/api/v1/auth/callback")
            .scope("openid", "email")
            .authorizationUri("https://kc/auth")
            .tokenUri("https://kc/token")
            .jwkSetUri("https://kc/certs")
            .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            ISSUED,
            EXPIRES,
            Set.of("openid", "email")
        );
        return new OAuth2AuthorizedClient(
            registration,
            "ada",
            accessToken,
            new OAuth2RefreshToken("refresh-token", ISSUED)
        );
    }
}
