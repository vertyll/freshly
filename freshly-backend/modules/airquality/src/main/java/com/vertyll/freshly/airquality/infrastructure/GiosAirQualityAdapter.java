package com.vertyll.freshly.airquality.infrastructure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.airquality.domain.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.AirQualityProvider;
import com.vertyll.freshly.airquality.domain.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.Station;
import com.vertyll.freshly.airquality.infrastructure.GiosApiDtos.*;
import com.vertyll.freshly.common.config.ExternalServiceProperties;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@SuppressWarnings({
    "PMD.GodClass", // Adapter requires complex mapping logic for external API integration
    "PMD.AvoidCatchingGenericException" // External API - Jackson can throw various unknown
    // exceptions
})
class GiosAirQualityAdapter implements AirQualityProvider {

    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String USER_AGENT_VALUE = "Freshly-App/1.0";

    private static final String URI_STATION_FIND_ALL = "/station/findAll";
    private static final String URI_AQ_INDEX = "/aqindex/getIndex/{stationId}";
    private static final String URI_STATION_SENSORS = "/station/sensors/{stationId}";
    private static final String URI_SENSOR_DATA = "/data/getData/{sensorId}";

    private static final String JSON_PATH_STATIONS_LIST = "Lista stacji pomiarowych";
    private static final String JSON_PATH_LIST_LOWERCASE = "lista";
    private static final String JSON_PATH_LIST = "list";
    private static final String JSON_PATH_DATA = "data";
    private static final String JSON_PATH_AQ_INDEX = "AqIndex";
    private static final String JSON_PATH_AQ_INDEX_PL = "Indeks jakości powietrza";
    private static final String JSON_PATH_SENSORS_LIST_FULL =
            "Lista stanowisk pomiarowych dla podanej stacji";
    private static final String JSON_PATH_SENSORS_LIST = "Lista stanowisk pomiarowych";
    private static final String JSON_PATH_SENSORS_LIST_SHORT = "Lista stanowisk";
    private static final String JSON_PATH_MEASUREMENT_DATA = "Lista danych pomiarowych";
    private static final String JSON_PATH_VALUES = "values";
    private static final String JSON_PATH_MEASUREMENT_DATA_SHORT = "Dane pomiarowe";

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String COMMA = ",";
    private static final String DOT = ".";
    private static final String UNKNOWN_PARAMETER = "Nieznany parametr";
    private static final String NO_DATA = "Brak danych";
    private static final String PARAM_NA = "N/A";

    private static final double DEFAULT_COORDINATE = 0.0;
    private static final int DEFAULT_STATION_ID = 0;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    GiosAirQualityAdapter(ExternalServiceProperties externalServiceProperties) {
        this.restClient =
                RestClient.builder()
                        .baseUrl(externalServiceProperties.gios().apiUrl())
                        .defaultHeader(USER_AGENT_HEADER, USER_AGENT_VALUE)
                        .build();

        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Station> findAllStations() {
        String response = restClient.get().uri(URI_STATION_FIND_ALL).retrieve().body(String.class);

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
        JsonNode listNode = root.findPath(JSON_PATH_STATIONS_LIST);
        if (listNode.isMissingNode()) listNode = root.findPath(JSON_PATH_LIST_LOWERCASE);
        if (listNode.isMissingNode()) listNode = root.findPath(JSON_PATH_LIST);
        if (listNode.isMissingNode()) listNode = root.findPath(JSON_PATH_DATA);

        // Jeśli root sam jest tablicą, użyj go bezpośrednio
        if (listNode.isMissingNode() && root.isArray()) {
            listNode = root;
        }

        if (!listNode.isArray()) {
            log.warn("Not found stations list in response");
            return List.of();
        }

        try {
            List<GiosStationDto> dtos =
                    objectMapper.readerForListOf(GiosStationDto.class).readValue(listNode);
            return dtos.stream().map(this::mapToStation).toList();
        } catch (Exception e) {
            log.error("Error mapping stations", e);
            return List.of();
        }
    }

    @Override
    public Optional<AirQualityIndex> findIndexByStationId(int stationId) {
        String response =
                restClient.get().uri(URI_AQ_INDEX, stationId).retrieve().body(String.class);

        if (response == null) return Optional.empty();

        try {
            JsonNode root = objectMapper.readTree(response);
            log.debug("GIOŚ index response for station {}: {}", stationId, root);

            JsonNode indexNode = root.findPath(JSON_PATH_AQ_INDEX);
            if (indexNode.isMissingNode()) indexNode = root.findPath(JSON_PATH_AQ_INDEX_PL);
            if (indexNode.isMissingNode()) indexNode = root;

            // Zabezpieczenie: jeśli węzeł jest tablicą (czasami API tak zwraca), bierzemy pierwszy
            // element
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

            measurements.add(
                    new SensorMeasurement(
                            sensor.id(),
                            sensor.paramCode() != null ? sensor.paramCode() : PARAM_NA,
                            sensor.paramName() != null ? sensor.paramName() : UNKNOWN_PARAMETER,
                            readings));
        }

        return measurements;
    }

    private List<GiosSensorDto> fetchSensors(int stationId) {
        try {
            String response =
                    restClient
                            .get()
                            .uri(URI_STATION_SENSORS, stationId)
                            .retrieve()
                            .body(String.class);

            if (response == null) return List.of();

            JsonNode root = objectMapper.readTree(response);
            log.debug("GIOŚ sensors response for station {}: {}", stationId, root);

            JsonNode listNode = root.findPath(JSON_PATH_SENSORS_LIST_FULL);
            if (listNode.isMissingNode()) listNode = root.findPath(JSON_PATH_SENSORS_LIST);
            if (listNode.isMissingNode()) listNode = root.findPath(JSON_PATH_SENSORS_LIST_SHORT);
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
            String response =
                    restClient.get().uri(URI_SENSOR_DATA, sensorId).retrieve().body(String.class);

            if (response == null) return List.of();

            JsonNode root = objectMapper.readTree(response);
            log.debug("GIOŚ data response for sensor {}: {}", sensorId, root);

            JsonNode valuesNode = root.findPath(JSON_PATH_MEASUREMENT_DATA);
            if (valuesNode.isMissingNode()) valuesNode = root.findPath(JSON_PATH_VALUES);
            if (valuesNode.isMissingNode())
                valuesNode = root.findPath(JSON_PATH_MEASUREMENT_DATA_SHORT);
            if (valuesNode.isMissingNode()) valuesNode = root.findPath(JSON_PATH_LIST_LOWERCASE);
            if (valuesNode.isMissingNode()) valuesNode = root.findPath(JSON_PATH_DATA);

            if (valuesNode.isArray()) {
                List<GiosDataValueDto> values =
                        objectMapper.readerForListOf(GiosDataValueDto.class).readValue(valuesNode);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

                return values.stream()
                        .filter(v -> v.value() != null) // Filtrujemy nulle (częste w GIOŚ)
                        .map(
                                v -> {
                                    try {
                                        return new SensorMeasurement.Reading(
                                                LocalDateTime.parse(v.date(), formatter),
                                                v.value());
                                    } catch (Exception _) {
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
                dto.id() != null ? dto.id() : DEFAULT_STATION_ID,
                dto.stationName(),
                dto.cityName() != null ? dto.cityName() : "",
                dto.addressStreet(),
                parseCoordinate(dto.gegrLat()),
                parseCoordinate(dto.gegrLon()));
    }

    private AirQualityIndex mapToDomain(GiosAQIndexDto dto, int stationId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        LocalDateTime date =
                dto.stCalcDate() != null
                        ? LocalDateTime.parse(dto.stCalcDate(), formatter)
                        : LocalDateTime.now(ZoneOffset.UTC);

        return new AirQualityIndex(
                stationId,
                date,
                getLevelName(dto.stIndexLevel()),
                getLevelName(dto.so2IndexLevel()),
                getLevelName(dto.no2IndexLevel()),
                getLevelName(dto.pm10IndexLevel()));
    }

    private String getLevelName(String levelName) {
        return levelName != null ? levelName : NO_DATA;
    }

    private double parseCoordinate(String value) {
        try {
            if (value == null) return DEFAULT_COORDINATE;
            // GIOŚ API może zwracać współrzędne z przecinkiem jako separatorem dziesiętnym
            String normalized = value.replace(COMMA.charAt(0), DOT.charAt(0));
            return Double.parseDouble(normalized);
        } catch (NumberFormatException _) {
            return DEFAULT_COORDINATE;
        }
    }
}
