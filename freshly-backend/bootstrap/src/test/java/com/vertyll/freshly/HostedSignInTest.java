package com.vertyll.freshly;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MongoTestContainer.class)
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
        MockHttpSession browser = new MockHttpSession();
        stateIssuedTo(browser);

        MvcTestResult result =
                mvc.get().uri("/auth/callback").session(browser).param(CODE, CODE).param("state", "forged").exchange();

        assertThat(result).hasStatus(HttpStatus.FOUND);
        assertThat(result.getResponse().getRedirectedUrl()).endsWith("error=state_mismatch");
    }

    @Test
    void signInProvisionsTheUserAndRelaysTheTokenFromTheSession() {
        when(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(jwt());

        MockHttpSession browser = signedIn(session(Instant.now().plusSeconds(300)));

        assertThat(users.findUser(SUBJECT)).isPresent();
        assertThat(mvc.get().uri("/auth/session").session(browser)).hasStatusOk()
            .bodyJson()
            .extractingPath("$.email")
            .isEqualTo("ada@freshly.local");
        assertThat(mvc.get().uri(USER_PATH).session(browser)).hasStatusOk();
        assertThat(mvc.get().uri(USER_PATH)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anExpiredSessionThatCannotBeRefreshedEnds() {
        when(tokenIssuer.refresh(anyString())).thenThrow(new DomainException(AuthError.SESSION_EXPIRED));
        MockHttpSession browser = signedIn(session(Instant.now().minusSeconds(5)));

        assertThat(mvc.get().uri(USER_PATH).session(browser)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(browser.isInvalid()).isTrue();
    }

    @Test
    void aCrossSiteWriteIsNotGivenTheSessionToken() {
        when(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(jwt());
        MockHttpSession browser = signedIn(session(Instant.now().plusSeconds(300)));

        assertThat(mvc.post().uri("/users").session(browser).header("Sec-Fetch-Site", "cross-site"))
            .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesTheRefreshTokenAndEndsTheSession() {
        MockHttpSession browser = signedIn(session(Instant.now().plusSeconds(300)));

        assertThat(mvc.post().uri("/auth/logout").session(browser)).hasStatus(HttpStatus.NO_CONTENT);
        verify(tokenIssuer).revoke(REFRESH_TOKEN);
        assertThat(browser.isInvalid()).isTrue();
    }

    private String stateIssuedTo(MockHttpSession browser) {
        String state = queryOf(mvc.get().uri("/auth/authorize").session(browser).exchange()).getFirst("state");
        assertThat(state).isNotNull();
        return state;
    }

    private MockHttpSession signedIn(AuthSession authSession) {
        when(tokenIssuer.exchange(eq(CODE), anyString())).thenReturn(authSession);
        MockHttpSession browser = new MockHttpSession();
        String state = stateIssuedTo(browser);

        MvcTestResult callback =
                mvc.get().uri("/auth/callback").session(browser).param(CODE, CODE).param("state", state).exchange();

        assertThat(callback).hasStatus(HttpStatus.FOUND);
        assertThat(callback.getResponse().getRedirectedUrl()).doesNotContain("error");
        assertThat(browser.isInvalid()).as("the pre-login session is replaced").isTrue();
        return (MockHttpSession) Objects.requireNonNull(callback.getRequest().getSession(false));
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
