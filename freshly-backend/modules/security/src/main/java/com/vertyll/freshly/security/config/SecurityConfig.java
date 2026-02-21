package com.vertyll.freshly.security.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.common.enums.UserRoleEnum;

@Slf4j
@Configuration
@Profile({"dev", "prod"})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, CorsConfigurationSource corsConfigurationSource) {

        return http.cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // Actuator
                                        .requestMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()

                                        // Auth public endpoints
                                        .requestMatchers(
                                                "/auth/register",
                                                "/auth/login",
                                                "/auth/refresh",
                                                "/auth/logout",
                                                "/auth/verify-email",
                                                "/auth/forgot-password",
                                                "/auth/reset-password")
                                        .permitAll()

                                        // Public AirQuality endpoints
                                        .requestMatchers("/air-quality/**")
                                        .permitAll()

                                        // Swagger UI
                                        .requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()

                                        // Actuator endpoints
                                        .requestMatchers(
                                                "/actuator/health",
                                                "/actuator/health/**",
                                                "/api/v1/actuator/health",
                                                "/api/v1/actuator/health/**")
                                        .permitAll()

                                        // All other endpoints require authentication
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(
                                        jwt ->
                                                jwt.jwtAuthenticationConverter(
                                                        jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        // Custom converter for extracting roles from Keycloak token
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return jwtConverter;
    }

    static class KeycloakRealmRoleConverter
            implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> realmAccess =
                    (Map<String, Object>) jwt.getClaims().get("realm_access");

            if (realmAccess == null || realmAccess.isEmpty()) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            final List<String> roles = (List<String>) realmAccess.get("roles");

            if (roles == null || roles.isEmpty()) {
                return List.of();
            }

            return roles.stream()
                    .map(roleName -> UserRoleEnum.ROLE_PREFIX + roleName)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
    }
}
