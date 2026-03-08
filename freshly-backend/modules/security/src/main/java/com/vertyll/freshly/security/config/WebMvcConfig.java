package com.vertyll.freshly.security.config;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

import com.vertyll.freshly.common.config.KeycloakProperties;
import com.vertyll.freshly.security.resolver.RefreshTokenCookieArgumentResolver;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final KeycloakProperties keycloakProperties;

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new RefreshTokenCookieArgumentResolver(keycloakProperties));
    }
}
