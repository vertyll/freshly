package com.vertyll.freshly;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.vertyll.freshly.auth.application.port.outbound.SessionRevocationPort;
import com.vertyll.freshly.useraccess.application.port.inbound.query.UserAccessQueryUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(
    {
        MongoTestContainer.class,
        RedisTestContainer.class
    }
)
class HostedSignInTest {
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESHED_ACCESS_TOKEN = "refreshed-access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String ID_TOKEN = "id-token";
    private static final String CODE = "code";
    private static final String EMAIL = "ada@freshly.local";
    private static final UUID SUBJECT = UUID.fromString("7d1c9a52-3a43-4c34-9a8f-2f5d7c6f1b10");
    private static final String USER_PATH = "/users/" + SUBJECT;
    private static final long LIFETIME_SECONDS = 300;
    private static final long EXPIRING_SECONDS = 5;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserAccessQueryUseCase users;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtDecoderFactory<ClientRegistration> idTokenDecoders;

    @MockitoBean
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> codeTokens;

    @MockitoBean
    private OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> refreshTokens;

    @MockitoBean
    private SessionRevocationPort revocation;

    @BeforeEach
    void keycloakIssuesTokens() {
        when(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(accessToken(ACCESS_TOKEN));
        when(jwtDecoder.decode(REFRESHED_ACCESS_TOKEN)).thenReturn(accessToken(REFRESHED_ACCESS_TOKEN));
    }

    @Test
    void authorizeRedirectsToKeycloakWithPkceAndLanguage() {
        MvcTestResult result = mvc.get()
            .uri("/auth/authorize")
            .param("register", "true")
            .param("kc_action", "UPDATE_PASSWORD")
            .header("Accept-Language", "pl")
            .exchange();

        assertThat(result).hasStatus(HttpStatus.FOUND);
        assertThat(result.getResponse().getRedirectedUrl())
            .startsWith("http://localhost:9000/realms/freshly/protocol/openid-connect/auth");
        MultiValueMap<String, String> query = queryOf(result);
        assertThat(query.get("code_challenge_method")).containsExactly("S256");
        assertThat(query.getFirst("code_challenge")).hasSize(43);
        assertThat(query.getFirst("state")).isNotBlank();
        assertThat(query.get("ui_locales")).containsExactly("pl");
        assertThat(query.get("prompt")).containsExactly("create");
        assertThat(query.get("kc_action")).containsExactly("UPDATE_PASSWORD");
        assertThat(query.get("redirect_uri")).containsExactly("http://localhost:8080/api/v1/auth/callback");
    }

    @Test
    void callbackRefusesAStateThisBrowserWasNotGiven() {
        Browser browser = new Browser();
        browser.send(mvc.get().uri("/auth/authorize"));

        MvcTestResult result = browser.send(mvc.get().uri("/auth/callback").param(CODE, CODE).param("state", "forged"));

        assertThat(result).hasStatus(HttpStatus.FOUND);
        assertThat(result.getResponse().getRedirectedUrl()).endsWith("error=state_mismatch");
    }

    @Test
    void signInProvisionsTheUserAndRelaysTheTokenFromTheSession() {
        Browser browser = signedIn(LIFETIME_SECONDS);

        assertThat(users.findUser(SUBJECT)).isPresent();
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatusOk()
            .bodyJson()
            .extractingPath("$.email")
            .isEqualTo(EMAIL);
        assertThat(browser.send(mvc.get().uri(USER_PATH))).hasStatusOk();
        assertThat(mvc.get().uri(USER_PATH)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aSignInWithAnUnusableTokenRevokesTheNewSession() {
        when(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(accessTokenWithSubject("not-a-uuid"));

        Browser browser = new Browser();
        MvcTestResult callback = completeSignIn(browser, LIFETIME_SECONDS);

        assertThat(callback.getResponse().getRedirectedUrl()).endsWith("error=sign_in_failed");
        verify(revocation).revoke(REFRESH_TOKEN);
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void aClientWithItsOwnTokenCallsTheApiWithoutASession() {
        signedIn(LIFETIME_SECONDS);

        assertThat(mvc.get().uri(USER_PATH).header("Authorization", "Bearer " + ACCESS_TOKEN)).hasStatusOk();
    }

    @Test
    void anAccessTokenAboutToExpireIsRefreshed() {
        when(refreshTokens.getTokenResponse(any())).thenReturn(tokenResponse(REFRESHED_ACCESS_TOKEN, LIFETIME_SECONDS));
        Browser browser = signedIn(EXPIRING_SECONDS);

        assertThat(browser.send(mvc.get().uri(USER_PATH))).hasStatusOk();
        verify(refreshTokens).getTokenResponse(any());
    }

    @Test
    void anExpiredSessionThatCannotBeRefreshedEnds() {
        when(refreshTokens.getTokenResponse(any()))
            .thenThrow(new OAuth2AuthorizationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT)));
        Browser browser = signedIn(EXPIRING_SECONDS);

        assertThat(browser.send(mvc.get().uri(USER_PATH))).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void aCrossSiteWriteIsNotGivenTheSessionToken() {
        Browser browser = signedIn(LIFETIME_SECONDS);

        assertThat(browser.send(mvc.post().uri("/users").header("Sec-Fetch-Site", "cross-site")))
            .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesTheRefreshTokenAndEndsTheSession() {
        Browser browser = signedIn(LIFETIME_SECONDS);

        assertThat(browser.send(mvc.post().uri("/auth/logout"))).hasStatus(HttpStatus.NO_CONTENT);
        verify(revocation).revoke(REFRESH_TOKEN);
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void logoutSentFromAnotherSiteLeavesTheSession() {
        Browser browser = signedIn(LIFETIME_SECONDS);

        assertThat(browser.send(mvc.post().uri("/auth/logout").header("Sec-Fetch-Site", "same-site")))
            .hasStatus(HttpStatus.FORBIDDEN);
        verify(revocation, never()).revoke(REFRESH_TOKEN);
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.OK);
    }

    private Browser signedIn(long accessTokenLifetimeSeconds) {
        Browser browser = new Browser();
        String state = keycloakAnswers(browser, accessTokenLifetimeSeconds);
        List<String> preLoginSession = browser.sessionValues();

        MvcTestResult callback = callback(browser, state);

        assertThat(callback).hasStatus(HttpStatus.FOUND);
        assertThat(callback.getResponse().getRedirectedUrl()).doesNotContain("error");
        assertThat(browser.sessionValues()).as("the pre-login session is replaced")
            .doesNotContainAnyElementsOf(preLoginSession);
        return browser;
    }

    private MvcTestResult completeSignIn(Browser browser, long accessTokenLifetimeSeconds) {
        return callback(browser, keycloakAnswers(browser, accessTokenLifetimeSeconds));
    }

    private MvcTestResult callback(Browser browser, String state) {
        return browser.send(mvc.get().uri("/auth/callback").param(CODE, CODE).param("state", state));
    }

    private String keycloakAnswers(Browser browser, long accessTokenLifetimeSeconds) {
        MultiValueMap<String, String> authorization = queryOf(browser.send(mvc.get().uri("/auth/authorize")));
        String nonce = authorization.getFirst("nonce");
        String state = authorization.getFirst("state");
        assertThat(nonce).isNotNull();
        assertThat(state).isNotNull();
        Jwt idToken = idToken(nonce);
        when(idTokenDecoders.createDecoder(any())).thenReturn(token -> idToken);
        when(codeTokens.getTokenResponse(any())).thenReturn(tokenResponse(ACCESS_TOKEN, accessTokenLifetimeSeconds));
        return state;
    }

    private static final class Browser {
        private final Map<String, Cookie> cookies = new LinkedHashMap<>();

        MvcTestResult send(MockMvcTester.MockMvcRequestBuilder request) {
            if (!cookies.isEmpty()) {
                request.cookie(cookies.values().toArray(Cookie[]::new));
            }
            MvcTestResult result = request.exchange();
            for (Cookie issued : result.getResponse().getCookies()) {
                if (issued.getMaxAge() == 0) {
                    cookies.remove(issued.getName());
                } else {
                    cookies.put(issued.getName(), issued);
                }
            }
            return result;
        }

        List<String> sessionValues() {
            return cookies.values().stream().map(Cookie::getValue).toList();
        }
    }

    private static OAuth2AccessTokenResponse tokenResponse(String accessToken, long lifetimeSeconds) {
        return OAuth2AccessTokenResponse.withToken(accessToken)
            .tokenType(OAuth2AccessToken.TokenType.BEARER)
            .expiresIn(lifetimeSeconds)
            .refreshToken(REFRESH_TOKEN)
            .scopes(Set.of("openid", "profile", "email"))
            .additionalParameters(Map.of(OidcParameterNames.ID_TOKEN, ID_TOKEN))
            .build();
    }

    private static Jwt idToken(String nonce) {
        return Jwt.withTokenValue(ID_TOKEN)
            .header("alg", "RS256")
            .issuer("http://localhost:9000/realms/freshly")
            .subject(SUBJECT.toString())
            .audience(List.of("freshly-app-client"))
            .claim("email", EMAIL)
            .claim("nonce", nonce)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(LIFETIME_SECONDS))
            .build();
    }

    private static Jwt accessToken(String value) {
        return accessToken(value, SUBJECT.toString());
    }

    private static Jwt accessTokenWithSubject(String subject) {
        return accessToken(ACCESS_TOKEN, subject);
    }

    private static Jwt accessToken(String value, String subject) {
        return Jwt.withTokenValue(value)
            .header("alg", "RS256")
            .subject(subject)
            .claim("email", EMAIL)
            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(LIFETIME_SECONDS))
            .build();
    }

    private static MultiValueMap<String, String> queryOf(MvcTestResult result) {
        String location = result.getResponse().getRedirectedUrl();
        assertThat(location).isNotNull();
        MultiValueMap<String, String> decoded = new LinkedMultiValueMap<>();
        UriComponentsBuilder.fromUri(URI.create(location))
            .build()
            .getQueryParams()
            .forEach(
                (name, values) -> values
                    .forEach(value -> decoded.add(name, URLDecoder.decode(value, StandardCharsets.UTF_8)))
            );
        return decoded;
    }
}
