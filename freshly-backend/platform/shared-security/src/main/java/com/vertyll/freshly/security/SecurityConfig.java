package com.vertyll.freshly.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
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

    @Bean
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CorsConfigurationSource corsConfigurationSource,
        PublicEndpointRegistry publicEndpoints,
        ProblemAuthenticationEntryPoint problemEntryPoint,
        Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter
    ) throws Exception {

        return http.cors(cors -> cors.configurationSource(corsConfigurationSource))
            // Safe only because the API is token-authenticated and stateless. The refresh
            // token travels in a cookie, which would normally reintroduce CSRF risk —
            // it does not here because the cookie is SameSite=Lax and scoped to /auth,
            // and no state-changing endpoint outside /auth reads it.
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                auth -> auth.requestMatchers(INFRASTRUCTURE_PATHS)
                    .permitAll()
                    .requestMatchers(publicEndpoints.matchers().toArray(RequestMatcher[]::new))
                    .permitAll()
                    // Everything else needs a token. Whether the holder may do the thing is
                    // decided by @RequirePermission at the method, not here — the two layers
                    // answer different questions and collapsing them into path patterns is
                    // how a permission check ends up living in a string.
                    .anyRequest()
                    .authenticated()
            )
            .oauth2ResourceServer(
                oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    // Without these, a rejection inside the filter chain answers with an
                    // empty body while the same rejection inside a controller answers with a
                    // problem document.
                    .authenticationEntryPoint(problemEntryPoint)
                    .accessDeniedHandler(problemEntryPoint)
            )
            .exceptionHandling(
                handling -> handling.authenticationEntryPoint(problemEntryPoint).accessDeniedHandler(problemEntryPoint)
            )
            .build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}
