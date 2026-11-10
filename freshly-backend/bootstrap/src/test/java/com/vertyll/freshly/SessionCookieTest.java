package com.vertyll.freshly;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The session cookie as a browser receives it, from the embedded server: under MockMvc
 * Spring Boot takes the cookie's settings from the mock servlet context instead of
 * {@code server.servlet.session.cookie}, so only a real request shows what is configured.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(
    {
        MongoTestContainer.class,
        RedisTestContainer.class
    }
)
class SessionCookieTest {

    @LocalServerPort
    private int port;

    @Test
    void theSessionCookieIsBoundToThisHost() throws IOException, InterruptedException {
        List<String> cookies;
        try (HttpClient http = HttpClient.newHttpClient()) {
            HttpResponse<Void> response = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/authorize")).build(),
                HttpResponse.BodyHandlers.discarding()
            );
            cookies = response.headers().allValues("Set-Cookie");
        }

        assertThat(cookies).filteredOn(cookie -> cookie.startsWith("__Host-FRESHLY_SESSION="))
            .singleElement()
            .satisfies(
                cookie -> assertThat(cookie).contains("; Path=/;", "; Secure", "; HttpOnly", "; SameSite=Lax")
                    .doesNotContain("Domain=")
            );
    }
}
