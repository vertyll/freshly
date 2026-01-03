package com.vertyll.freshly.airquality.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vertyll.freshly.airquality.domain.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.AirQualityProvider;
import com.vertyll.freshly.airquality.domain.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.Station;
import com.vertyll.freshly.airquality.infrastructure.GiosApiDtos.*;
import com.vertyll.freshly.common.config.ExternalServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
class GiosAirQualityAdapter implements AirQualityProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    GiosAirQualityAdapter(ExternalServiceProperties externalServiceProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(externalServiceProperties.gios().apiUrl())
                .defaultHeader("User-Agent", "Freshly-App/1.0")
                .build();

        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Station> findAllStations() {
        String response = restClient.get()
                .uri("/station/findAll")
                .retrieve()
                .body(String.class);

        if (response == null) {
            return List.of();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Error parsing stations response", e);
            return List.of();
        }

        log.debug("GIOŚ stations response: {}", root);

        // Próba znalezienia listy pod popularnymi kluczami w API GIOŚ
        JsonNode listNode = root.findPath("Lista stacji pomiarowych");
        if (listNode.isMissingNode()) listNode = root.findPath("lista");
        if (listNode.isMissingNode()) listNode = root.findPath("list");
        if (listNode.isMissingNode()) listNode = root.findPath("data");

        if (!listNode.isArray()) {
            log.warn("Not found stations list in response");
            return List.of();
        }

        try {
            List<GiosStationDto> dtos = objectMapper.readerForListOf(GiosStationDto.class).readValue(listNode);
            return dtos.stream()
                    .map(this::mapToStation)
                    .toList();
        } catch (Exception e) {
            log.error("Error mapping stations", e);
            return List.of();
        }
    }

    @Override
    public Optional<AirQualityIndex> findIndexByStationId(int stationId) {
        String response = restClient.get()
                .uri("/aqindex/getIndex/{stationId}", stationId)
                .retrieve()
                .body(String.class);

        if (response == null) return Optional.empty();

        try {
            JsonNode root = objectMapper.readTree(response);
            log.debug("GIOŚ index response for station {}: {}", stationId, root);

            JsonNode indexNode = root.findPath("AqIndex");
            if (indexNode.isMissingNode()) indexNode = root.findPath("Indeks jakości powietrza");
            if (indexNode.isMissingNode()) indexNode = root;

            // Zabezpieczenie: jeśli węzeł jest tablicą (czasami API tak zwraca), bierzemy pierwszy element
            if (indexNode.isArray()) {
                if (indexNode.isEmpty()) return Optional.empty();
                indexNode = indexNode.get(0);
            }

            GiosAQIndexDto dto = objectMapper.treeToValue(indexNode, GiosAQIndexDto.class);

            // Jeśli API zwróciło obiekt z samymi nullami (brak ID), traktujemy to jako brak danych
            if (dto == null || dto.id() == null) {
                return Optional.empty();
            }
            return Optional.of(dto).map(d -> mapToDomain(d, stationId));
        } catch (Exception e) {
            log.error("Error parsing air quality index for station {}", stationId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<SensorMeasurement> findMeasurementsByStationId(int stationId) {
        // 1. Pobierz listę sensorów dla stacji
        List<GiosSensorDto> sensors = fetchSensors(stationId);
        List<SensorMeasurement> measurements = new ArrayList<>();

        // 2. Dla każdego sensora pobierz dane
        for (GiosSensorDto sensor : sensors) {
            if (sensor.id() == null) continue;

            List<SensorMeasurement.Reading> readings = fetchDataForSensor(sensor.id());

            measurements.add(new SensorMeasurement(
                    sensor.id(),
                    sensor.paramCode() != null ? sensor.paramCode() : "N/A",
                    sensor.paramName() != null ? sensor.paramName() : "Nieznany parametr",
                    readings
            ));
        }

        return measurements;
    }

    private List<GiosSensorDto> fetchSensors(int stationId) {
        try {
            String response = restClient.get()
                    .uri("/station/sensors/{stationId}", stationId)
                    .retrieve()
                    .body(String.class);

            if (response == null) return List.of();

            JsonNode root = objectMapper.readTree(response);
            log.debug("GIOŚ sensors response for station {}: {}", stationId, root);

            JsonNode listNode = root.findPath("Lista stanowisk pomiarowych dla podanej stacji");
            if (listNode.isMissingNode()) listNode = root.findPath("Lista stanowisk pomiarowych");
            if (listNode.isMissingNode()) listNode = root.findPath("Lista stanowisk");
            if (listNode.isMissingNode()) listNode = root;

            if (listNode.isArray()) {
                return objectMapper.readerForListOf(GiosSensorDto.class).readValue(listNode);
            }
        } catch (Exception e) {
            log.error("Error fetching sensors for station {}", stationId, e);
        }
        return List.of();
    }

    private List<SensorMeasurement.Reading> fetchDataForSensor(int sensorId) {
        try {
            String response = restClient.get()
                    .uri("/data/getData/{sensorId}", sensorId)
                    .retrieve()
                    .body(String.class);

            if (response == null) return List.of();

            JsonNode root = objectMapper.readTree(response);
            log.debug("GIOŚ data response for sensor {}: {}", sensorId, root);

            JsonNode valuesNode = root.findPath("Lista danych pomiarowych"); // Klucz w nowym API
            if (valuesNode.isMissingNode()) valuesNode = root.findPath("values");
            if (valuesNode.isMissingNode()) valuesNode = root.findPath("Dane pomiarowe");
            if (valuesNode.isMissingNode()) valuesNode = root.findPath("lista");
            if (valuesNode.isMissingNode()) valuesNode = root.findPath("data");

            if (valuesNode.isArray()) {
                List<GiosDataValueDto> values = objectMapper.readerForListOf(GiosDataValueDto.class).readValue(valuesNode);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                return values.stream()
                        .filter(v -> v.value() != null) // Filtrujemy nulle (częste w GIOŚ)
                        .map(v -> {
                            try {
                                return new SensorMeasurement.Reading(
                                        LocalDateTime.parse(v.date(), formatter),
                                        v.value()
                                );
                            } catch (Exception e) {
                                return null; // Ignorujemy błędne daty
                            }
                        })
                        .filter(Objects::nonNull)
                        .toList();
            }
        } catch (Exception e) {
            log.error("Error fetching data for sensor {}", sensorId, e);
        }
        return List.of();
    }

    private Station mapToStation(GiosStationDto dto) {
        return new Station(
                dto.id() != null ? dto.id() : 0,
                dto.stationName(),
                dto.cityName() != null ? dto.cityName() : "",
                dto.addressStreet(),
                parseCoordinate(dto.gegrLat()),
                parseCoordinate(dto.gegrLon())
        );
    }

    private AirQualityIndex mapToDomain(GiosAQIndexDto dto, int stationId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime date = dto.stCalcDate() != null ? LocalDateTime.parse(dto.stCalcDate(), formatter) : LocalDateTime.now(ZoneOffset.UTC);

        return new AirQualityIndex(
                stationId,
                date,
                getLevelName(dto.stIndexLevel()),
                getLevelName(dto.so2IndexLevel()),
                getLevelName(dto.no2IndexLevel()),
                getLevelName(dto.pm10IndexLevel())
        );
    }

    private String getLevelName(String levelName) {
        return levelName != null ? levelName : "Brak danych";
    }

    private double parseCoordinate(String value) {
        try {
            return value != null ? Double.parseDouble(value) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}