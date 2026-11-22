package com.vertyll.freshly;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The CSRF cookie as the {@code local} profile sets it over plain HTTP, with the
 * {@code test} profile's settings over the rest of that profile.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(
    {
        "local",
        "test"
    }
)
@Import(TestcontainersConfiguration.class)
class LocalCsrfCookieTest {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";

    @LocalServerPort
    private int port;

    @Value("${springdoc.swagger-ui.csrf.cookie-name}")
    private String swaggerUiCookie;

    @Test
    void theCsrfCookieDropsThePrefixAndSecure() throws IOException, InterruptedException {
        List<String> cookies;
        try (HttpClient http = HttpClient.newHttpClient()) {
            HttpResponse<Void> response = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/session")).build(),
                HttpResponse.BodyHandlers.discarding()
            );
            cookies = response.headers().allValues("Set-Cookie");
        }

        assertThat(cookies).noneMatch(cookie -> cookie.startsWith("__Host-" + CSRF_COOKIE + "="));
        assertThat(cookies).filteredOn(cookie -> cookie.startsWith(CSRF_COOKIE + "="))
            .singleElement()
            .satisfies(
                cookie -> assertThat(cookie).contains("; Path=/;", "; SameSite=Lax")
                    .doesNotContain("Secure", "HttpOnly", "Domain=")
            );
    }

    @Test
    void swaggerUiReadsTheCsrfCookieByItsName() {
        assertThat(swaggerUiCookie).isEqualTo(CSRF_COOKIE);
    }
}
