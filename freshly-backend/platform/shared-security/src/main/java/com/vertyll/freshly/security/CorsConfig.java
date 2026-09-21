package com.vertyll.freshly.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    private static final String ALL_PATHS = "/**";

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(properties.allowedMethods());
        configuration.setAllowedHeaders(List.of(CorsConfiguration.ALL));

        // Required for the refresh-token cookie to be sent at all, and the reason the
        // origin list must be explicit — `allowCredentials` with a wildcard origin is
        // rejected by browsers, and working around that by echoing the request's origin
        // back is how a CORS policy stops being one.
        configuration.setAllowCredentials(true);

        // The client needs to read the ETag to send it back as If-Match; a header not
        // exposed here is invisible to JavaScript even when it is on the response.
        configuration.setExposedHeaders(List.of(HttpHeaders.ETAG, HttpHeaders.LOCATION));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ALL_PATHS, configuration);
        return source;
    }
}
