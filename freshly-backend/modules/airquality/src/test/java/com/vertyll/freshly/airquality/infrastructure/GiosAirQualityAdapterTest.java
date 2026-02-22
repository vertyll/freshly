package com.vertyll.freshly.airquality.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.vertyll.freshly.airquality.domain.SensorMeasurement;
import com.vertyll.freshly.common.config.ExternalServiceProperties;

class GiosAirQualityAdapterTest {

    private GiosAirQualityAdapter adapter;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        ExternalServiceProperties properties =
                new ExternalServiceProperties(
                        new ExternalServiceProperties.Gios("http://api.gios.gov.pl/pjp-api/rest"));
        restClient = mock(RestClient.class);
        adapter = new GiosAirQualityAdapter(properties);
        adapter.setRestClient(restClient);
    }

    @Test
    void shouldFallbackToArchivalDataWhenGiosReturns400ForManualStation() {
        // Given
        int sensorId = 654;
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), anyInt())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);

        // Pierwsze wywołanie (bieżące dane) rzuca 400
        // Drugie wywołanie (archiwalne dane) zwraca sukces
        String errorJson = "{\"error_code\":\"API-ERR-100003\"}";
        when(responseSpec.body(String.class))
                .thenThrow(
                        HttpClientErrorException.create(
                                HttpStatus.BAD_REQUEST,
                                "Bad Request",
                                null,
                                errorJson.getBytes(StandardCharsets.UTF_8),
                                null))
                .thenReturn("{\"values\": [{\"date\": \"2026-02-22 07:00:00\", \"value\": 25.5}]}");

        // When
        List<SensorMeasurement.Reading> readings = adapter.fetchDataForSensor(sensorId);

        // Then
        assertThat(readings).hasSize(1);
        assertThat(readings.getFirst().value()).isEqualTo(25.5);
    }
}
