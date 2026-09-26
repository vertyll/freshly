package com.vertyll.freshly.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Opens everything, for slice tests that are not about security.
 *
 * <p>
 * Being a {@code @TestConfiguration} in a test source set is what makes it
 * unreachable from production rather than merely inconvenient to reach.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    SecurityFilterChain permissiveFilterChain(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }
}
