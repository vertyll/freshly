package com.vertyll.freshly.airquality.infrastructure.gios;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.vertyll.freshly.airquality.domain.error.AirQualityError;
import com.vertyll.freshly.airquality.domain.model.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.model.AirQualityLevel;
import com.vertyll.freshly.airquality.domain.model.Pollutant;
import com.vertyll.freshly.airquality.domain.model.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.model.Station;
import com.vertyll.freshly.lang.error.DomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GiosAirQualityAdapterTest {
    private static final String BASE = "https://api.gios.gov.pl/pjp-api/v1/rest";
    private static final MediaType JSON_LD = MediaType.parseMediaType("application/ld+json");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private MockRestServiceServer server;
    private GiosAirQualityAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new GiosAirQualityAdapter(builder.build());
    }

    @Test
    @DisplayName("reads the station list from the v1 envelope, with its Polish keys")
    void readsStations() {
        expect("/station/findAll?page=0&size=500", fixture("stations.json"));

        List<Station> stations = adapter.findAllStations();

        assertThat(stations).hasSize(2);
        Station first = stations.getFirst();
        assertThat(first.id()).isEqualTo(552);
        assertThat(first.name()).isEqualTo("Warszawa, ul. Kondratowicza");
        assertThat(first.city()).isEqualTo("Warszawa");
        assertThat(first.address()).isEqualTo("ul. Kondratowicza 8");
        assertThat(first.coordinates().latitude()).isEqualTo(52.290_864);
        server.verify();
    }

    @Test
    @DisplayName("follows totalPages until the list is complete")
    void followsPages() {
        expect("/station/findAll?page=0&size=500", stationPage(1, 2));
        expect("/station/findAll?page=1&size=500", stationPage(2, 2));

        assertThat(adapter.findAllStations()).extracting(Station::id).containsExactly(1, 2);
        server.verify();
    }

    @Test
    @DisplayName("skips pollutants it does not chart and keeps a null hour as a null reading")
    void readsSensors() {
        expect("/station/sensors/552", fixture("sensors.json"));
        expect("/data/getData/3764?page=0&size=24", fixture("data.json"));

        List<SensorMeasurement> sensors = adapter.findSensorMeasurements(552);

        assertThat(sensors).hasSize(1);
        SensorMeasurement pm10 = sensors.getFirst();
        assertThat(pm10.pollutant()).isEqualTo(Pollutant.PM10);
        assertThat(pm10.readings()).hasSize(3);
        assertThat(pm10.readings().getFirst().value()).isNull();
        assertThat(pm10.readings().get(1).value()).isEqualTo(0.8);
        assertThat(pm10.readings().get(1).measuredAt())
            .isEqualTo(LocalDateTime.of(2025, 7, 4, 14, 0).atZone(WARSAW).toInstant());
        server.verify();
    }

    @Test
    @DisplayName("reads the index envelope, O3 included")
    void readsIndex() {
        expect("/aqindex/getIndex/552", fixture("index.json"));

        AirQualityIndex index = adapter.findCurrentIndex(552).orElseThrow();

        assertThat(index.overall()).isEqualTo(AirQualityLevel.GOOD);
        assertThat(index.so2()).isNull();
        assertThat(index.no2()).isEqualTo(AirQualityLevel.VERY_GOOD);
        assertThat(index.o3()).isEqualTo(AirQualityLevel.GOOD);
        assertThat(index.calculatedAt()).isEqualTo(LocalDateTime.of(2025, 7, 4, 14, 35, 10).atZone(WARSAW).toInstant());
        server.verify();
    }

    @Test
    @DisplayName("retries once after the wait a 429 asks for")
    void retriesAfterThrottling() {
        server.expect(requestTo(BASE + "/aqindex/getIndex/552"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "0"));
        expect("/aqindex/getIndex/552", fixture("index.json"));

        assertThat(adapter.findCurrentIndex(552)).isPresent();
        server.verify();
    }

    @Test
    @DisplayName("an envelope without its list is a provider fault, not an empty result")
    void refusesMissingList() {
        expect("/station/sensors/552", "{\"meta\": {}}");

        assertThatThrownBy(() -> adapter.findSensorMeasurements(552)).isInstanceOf(DomainException.class)
            .extracting(e -> ((DomainException) e).error())
            .isEqualTo(AirQualityError.PROVIDER_RESPONSE_INVALID);
    }

    private void expect(String path, String body) {
        server.expect(requestTo(BASE + path)).andRespond(withSuccess(body, JSON_LD));
    }

    private static String stationPage(int id, int totalPages) {
        return "{\"Lista stacji pomiarowych\": [{\"Identyfikator stacji\": %d, \"Nazwa stacji\": \"Stacja %d\", "
            .formatted(id, id)
                + "\"WGS84 \u03c6 N\": \"52.0\", \"WGS84 \u03bb E\": \"21.0\"}], \"totalPages\": %d}"
                    .formatted(totalPages);
    }

    private static String fixture(String name) {
        try (InputStream input = GiosAirQualityAdapterTest.class.getResourceAsStream("/gios/" + name)) {
            if (input == null) {
                throw new IllegalStateException("Missing fixture " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
