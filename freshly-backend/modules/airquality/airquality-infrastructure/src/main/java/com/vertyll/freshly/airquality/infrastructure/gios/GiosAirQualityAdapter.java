package com.vertyll.freshly.airquality.infrastructure.gios;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.vertyll.freshly.airquality.application.port.outbound.AirQualityProviderPort;
import com.vertyll.freshly.airquality.domain.error.AirQualityError;
import com.vertyll.freshly.airquality.domain.model.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.model.Pollutant;
import com.vertyll.freshly.airquality.domain.model.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.model.Station;
import com.vertyll.freshly.airquality.infrastructure.config.GiosRestClientConfig;
import com.vertyll.freshly.lang.error.DomainException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GiosAirQualityAdapter implements AirQualityProviderPort {
    private static final String ALL_STATIONS = "/station/findAll?page={page}&size={size}";
    private static final String STATION_SENSORS = "/station/sensors/{stationId}";
    private static final String SENSOR_DATA = "/data/getData/{sensorId}?page=0&size={size}";
    private static final String STATION_INDEX = "/aqindex/getIndex/{stationId}";

    private static final int STATION_PAGE_SIZE = 500;

    private static final int READINGS_PER_SENSOR = 24;

    private static final Duration MIN_REQUEST_INTERVAL = Duration.ofMillis(60);

    private static final Duration MAX_RETRY_AFTER = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final Lock pacing = new ReentrantLock();
    private long lastRequestNanos;

    public GiosAirQualityAdapter(@Qualifier(GiosRestClientConfig.GIOS_REST_CLIENT) RestClient giosRestClient) {
        this.restClient = giosRestClient;
    }

    @Override
    public List<Station> findAllStations() {
        List<Station> stations = new ArrayList<>();
        int page = 0;
        int totalPages;

        do {
            GiosResponses.StationPage response = required(
                get(ALL_STATIONS, GiosResponses.StationPage.class, Map.of("page", page, "size", STATION_PAGE_SIZE)),
                ALL_STATIONS
            );
            required(response.stations(), ALL_STATIONS).stream()
                .map(GiosTranslator::toStation)
                .flatMap(Optional::stream)
                .forEach(stations::add);
            totalPages = response.totalPages();
            page++;
        } while (page < totalPages);

        return List.copyOf(stations);
    }

    @Override
    public Optional<AirQualityIndex> findCurrentIndex(int stationId) {
        GiosResponses.IndexEnvelope envelope = required(
            get(STATION_INDEX, GiosResponses.IndexEnvelope.class, Map.of("stationId", stationId)),
            STATION_INDEX
        );
        return GiosTranslator.toIndex(stationId, required(envelope.index(), STATION_INDEX));
    }

    @Override
    public List<SensorMeasurement> findSensorMeasurements(int stationId) {
        GiosResponses.SensorList response = required(
            get(STATION_SENSORS, GiosResponses.SensorList.class, Map.of("stationId", stationId)),
            STATION_SENSORS
        );

        return required(response.sensors(), STATION_SENSORS).stream()
            .map(this::toMeasurement)
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<SensorMeasurement> toMeasurement(GiosResponses.SensorDto sensor) {
        Optional<Pollutant> pollutant = GiosTranslator.pollutantOf(sensor);
        if (pollutant.isEmpty()) {
            return Optional.empty();
        }

        GiosResponses.DataPage page = required(
            get(
                SENSOR_DATA,
                GiosResponses.DataPage.class,
                Map.of("sensorId", sensor.id(), "size", READINGS_PER_SENSOR)
            ),
            SENSOR_DATA
        );
        return Optional
            .of(GiosTranslator.toMeasurement(sensor.id(), pollutant.get(), required(page.values(), SENSOR_DATA)));
    }

    private <T> @Nullable T get(String path, Class<T> type, Map<String, ?> variables) {
        try {
            return fetch(path, type, variables);
        } catch (HttpClientErrorException.TooManyRequests throttled) {
            sleep(retryAfter(throttled));
            try {
                return fetch(path, type, variables);
            } catch (RestClientException e) {
                e.addSuppressed(throttled);
                throw unavailable(path, e);
            }
        } catch (RestClientException e) {
            throw unavailable(path, e);
        }
    }

    private <T> @Nullable T fetch(String path, Class<T> type, Map<String, ?> variables) {
        pace();
        return restClient.get().uri(path, variables).retrieve().body(type);
    }

    private void pace() {
        pacing.lock();
        try {
            long wait = lastRequestNanos + MIN_REQUEST_INTERVAL.toNanos() - System.nanoTime();
            if (wait > 0) {
                sleep(Duration.ofNanos(wait));
            }
            lastRequestNanos = System.nanoTime();
        } finally {
            pacing.unlock();
        }
    }

    private static Duration retryAfter(HttpClientErrorException throttled) {
        HttpHeaders headers = throttled.getResponseHeaders();
        String value = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null) {
            return MAX_RETRY_AFTER;
        }
        try {
            Duration requested = Duration.ofSeconds(Long.parseLong(value.strip()));
            return requested.compareTo(MAX_RETRY_AFTER) > 0 ? MAX_RETRY_AFTER : requested;
        } catch (NumberFormatException _) {
            return MAX_RETRY_AFTER;
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DomainException(AirQualityError.PROVIDER_UNAVAILABLE, Map.of("path", "interrupted"), e);
        }
    }

    private static DomainException unavailable(String path, RestClientException cause) {
        log.warn("GIOŚ request to {} failed", path, cause);
        return new DomainException(AirQualityError.PROVIDER_UNAVAILABLE, Map.of("path", path), cause);
    }

    private static <T> T required(@Nullable T value, String path) {
        if (value == null) {
            throw new DomainException(AirQualityError.PROVIDER_RESPONSE_INVALID, Map.of("path", path));
        }
        return value;
    }
}
