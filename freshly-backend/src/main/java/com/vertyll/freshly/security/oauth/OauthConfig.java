package com.vertyll.freshly.security.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(OauthProperties.class)
public class OauthConfig {

    @Bean
    public RestClient keycloakClient(OauthProperties oauthProperties) {
        return RestClient.builder()
                .baseUrl(oauthProperties.getBaseUrl())
                .requestInterceptor(loggingInterceptor())
                .defaultStatusHandler(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, response) -> {
                            log.error("Keycloak error: {} - {}", response.getStatusCode(), response.getStatusText());
                        }
                )
                .build();
    }

    @Bean
    public ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.debug("Keycloak request: {} {}", request.getMethod(), request.getURI());

            var response = execution.execute(request, body);

            log.debug("Keycloak response: {}", response.getStatusCode());

            return response;
        };
    }

    @Bean
    public KeycloakTokenProvider keycloakTokenProvider(RestClient keycloakClient, OauthProperties oauthProperties) {
        return new KeycloakTokenProvider(keycloakClient, oauthProperties);
    }
}
