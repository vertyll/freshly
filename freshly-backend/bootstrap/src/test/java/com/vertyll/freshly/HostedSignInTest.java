package com.vertyll.freshly;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import com.vertyll.freshly.auth.application.port.outbound.TokenIssuerPort;
import com.vertyll.freshly.auth.domain.error.AuthError;
import com.vertyll.freshly.auth.domain.model.AuthSession;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.useraccess.application.port.inbound.query.UserAccessQueryUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String CODE = "code";
    private static final UUID SUBJECT = UUID.fromString("7d1c9a52-3a43-4c34-9a8f-2f5d7c6f1b10");
    private static final String USER_PATH = "/users/" + SUBJECT;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserAccessQueryUseCase users;

    @MockitoBean
    private TokenIssuerPort tokenIssuer;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void authorizeRedirectsToKeycloakWithPkceAndLanguage() {
        MvcTestResult result =
                mvc.get().uri("/auth/authorize").param("register", "true").header("Accept-Language", "pl").exchange();

        assertThat(result).hasStatus(HttpStatus.FOUND);
        assertThat(result.getResponse().getRedirectedUrl())
            .startsWith("http://localhost:9000/realms/freshly/protocol/openid-connect/auth");
        MultiValueMap<String, String> query = queryOf(result);
        assertThat(query.get("code_challenge_method")).containsExactly("S256");
        assertThat(query.getFirst("code_challenge")).hasSize(43);
        assertThat(query.getFirst("state")).isNotBlank();
        assertThat(query.get("ui_locales")).containsExactly("pl");
        assertThat(query.get("prompt")).containsExactly("create");
        assertThat(query.get("redirect_uri")).containsExactly("http://localhost:8080/api/v1/auth/callback");
    }

    @Test
    void callbackRefusesAStateThisBrowserWasNotGiven() {
        Browser browser = new Browser();
        stateIssuedTo(browser);

        MvcTestResult result = browser.send(mvc.get().uri("/auth/callback").param(CODE, CODE).param("state", "forged"));

        assertThat(result).hasStatus(HttpStatus.FOUND);
        assertThat(result.getResponse().getRedirectedUrl()).endsWith("error=state_mismatch");
    }

    @Test
    void signInProvisionsTheUserAndRelaysTheTokenFromTheSession() {
        when(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(jwt());

        Browser browser = signedIn(session(Instant.now().plusSeconds(300)));

        assertThat(users.findUser(SUBJECT)).isPresent();
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatusOk()
            .bodyJson()
            .extractingPath("$.email")
            .isEqualTo("ada@freshly.local");
        assertThat(browser.send(mvc.get().uri(USER_PATH))).hasStatusOk();
        assertThat(mvc.get().uri(USER_PATH)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anExpiredSessionThatCannotBeRefreshedEnds() {
        when(tokenIssuer.refresh(anyString())).thenThrow(new DomainException(AuthError.SESSION_EXPIRED));
        Browser browser = signedIn(session(Instant.now().minusSeconds(5)));

        assertThat(browser.send(mvc.get().uri(USER_PATH))).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void aCrossSiteWriteIsNotGivenTheSessionToken() {
        when(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(jwt());
        Browser browser = signedIn(session(Instant.now().plusSeconds(300)));

        assertThat(browser.send(mvc.post().uri("/users").header("Sec-Fetch-Site", "cross-site")))
            .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesTheRefreshTokenAndEndsTheSession() {
        Browser browser = signedIn(session(Instant.now().plusSeconds(300)));

        assertThat(browser.send(mvc.post().uri("/auth/logout"))).hasStatus(HttpStatus.NO_CONTENT);
        verify(tokenIssuer).revoke(REFRESH_TOKEN);
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void logoutSentFromAnotherSiteLeavesTheSession() {
        Browser browser = signedIn(session(Instant.now().plusSeconds(300)));

        assertThat(browser.send(mvc.post().uri("/auth/logout").header("Sec-Fetch-Site", "same-site")))
            .hasStatus(HttpStatus.FORBIDDEN);
        verify(tokenIssuer, never()).revoke(REFRESH_TOKEN);
        assertThat(browser.send(mvc.get().uri("/auth/session"))).hasStatus(HttpStatus.OK);
    }

    private String stateIssuedTo(Browser browser) {
        String state = queryOf(browser.send(mvc.get().uri("/auth/authorize"))).getFirst("state");
        assertThat(state).isNotNull();
        return state;
    }

    private Browser signedIn(AuthSession authSession) {
        when(tokenIssuer.exchange(eq(CODE), anyString())).thenReturn(authSession);
        Browser browser = new Browser();
        String state = stateIssuedTo(browser);
        List<String> preLoginSession = browser.sessionValues();

        MvcTestResult callback = browser.send(mvc.get().uri("/auth/callback").param(CODE, CODE).param("state", state));

        assertThat(callback).hasStatus(HttpStatus.FOUND);
        assertThat(callback.getResponse().getRedirectedUrl()).doesNotContain("error");
        assertThat(browser.sessionValues()).as("the pre-login session is replaced")
            .doesNotContainAnyElementsOf(preLoginSession);
        return browser;
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

    private static AuthSession session(Instant expiresAt) {
        return new AuthSession(SUBJECT, "ada@freshly.local", Set.of("ADMIN"), ACCESS_TOKEN, REFRESH_TOKEN, expiresAt);
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue(ACCESS_TOKEN)
            .header("alg", "RS256")
            .subject(SUBJECT.toString())
            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    }

    private static MultiValueMap<String, String> queryOf(MvcTestResult result) {
        String location = result.getResponse().getRedirectedUrl();
        assertThat(location).isNotNull();
        return UriComponentsBuilder.fromUri(URI.create(location)).build().getQueryParams();
    }
}
