package com.vertyll.freshly.security;

import java.util.List;

import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * The filter chain.
 *
 * <p>
 * <b>Carries no {@code @Profile}, deliberately.</b> Restricting it so a test
 * configuration can take over leaves the real chain inactive under every profile the
 * restriction does not name — including an unset one, where a deployment starts with no
 * security configuration at all and every endpoint reachable. A test configuration
 * belongs in a test source set, overriding by being on the test classpath rather than by
 * leaving a hole in the main one.
 */
@Configuration
public class SecurityConfig {
    private static final String[] INFRASTRUCTURE_PATHS = {
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health",
        "/actuator/health/**"
    };

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "Bearer";

    private static final String CSRF_COOKIE_SAME_SITE = "Lax";

    private static final String[] PUBLIC_PAGES = {
        "/legal/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        ServerProperties server,
        CorsConfigurationSource corsConfigurationSource,
        PublicEndpointRegistry publicEndpoints,
        ProblemAuthenticationEntryPoint problemEntryPoint,
        Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
        List<SecurityChainCustomizer> customizers
    ) {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(
                csrf -> csrf.spa()
                    .csrfTokenRepository(csrfTokenRepository(server.getServlet().getSession().getCookie()))
                    .ignoringRequestMatchers(new BearerTokenRequests())
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                auth -> auth.requestMatchers(INFRASTRUCTURE_PATHS)
                    .permitAll()
                    .requestMatchers(PUBLIC_PAGES)
                    .permitAll()
                    .requestMatchers(publicEndpoints.matchers().toArray(RequestMatcher[]::new))
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            )
            .oauth2ResourceServer(
                oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .authenticationEntryPoint(problemEntryPoint)
                    .accessDeniedHandler(problemEntryPoint)
            )
            .exceptionHandling(
                handling -> handling.authenticationEntryPoint(problemEntryPoint).accessDeniedHandler(problemEntryPoint)
            );
        customizers.forEach(customizer -> customizer.customize(http));
        return http.build();
    }

    /**
     * Refuses every Keycloak token that is not an access token, an ID token above all.
     *
     * <p>
     * The realm signs both with the same keys and sets the header's {@code typ} to
     * {@code JWT} on both, as it issues no RFC 9068 {@code at+jwt} header; its own
     * {@code typ} claim, {@code Bearer} on an access token and {@code ID} on an ID token, is
     * what tells them apart. Spring Boot adds this validator to the resource server's
     * issuer, audience and expiry checks.
     */
    @Bean
    OAuth2TokenValidator<Jwt> accessTokenTypeValidator() {
        return new JwtClaimValidator<>(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE::equals);
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    /**
     * The CSRF token in the {@code XSRF-TOKEN} cookie, which a page of this origin reads and
     * sends back in {@code X-XSRF-TOKEN}, as modules/auth/docs/mechanisms/cross-site-requests.md
     * explains.
     *
     * <p>
     * The cookie cannot be {@code HttpOnly}, since a script must read it; it holds no
     * credential, only the token a forged request cannot know. It is {@code Secure} exactly
     * when the session cookie is, so the local profile over plain HTTP gets it too.
     */
    private static CookieCsrfTokenRepository csrfTokenRepository(Cookie sessionCookie) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        Boolean secure = sessionCookie.getSecure();
        repository.setCookieCustomizer(cookie -> {
            cookie.sameSite(CSRF_COOKIE_SAME_SITE);
            if (secure != null) {
                cookie.secure(secure);
            }
        });
        return repository;
    }
}
