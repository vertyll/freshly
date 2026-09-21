package com.vertyll.freshly.airquality.infrastructure.config;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GiosRestClientConfig {
    public static final String GIOS_REST_CLIENT = "giosRestClient";
    public static final String GIOS_HTTP_CLIENT = "giosHttpClient";

    @Bean(name = GIOS_HTTP_CLIENT, destroyMethod = "close")
    HttpClient giosHttpClient(GiosProperties properties) {
        return HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Bean(GIOS_REST_CLIENT)
    RestClient giosRestClient(GiosProperties properties, @Qualifier(GIOS_HTTP_CLIENT) HttpClient httpClient) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
    }
}
