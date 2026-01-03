package com.vertyll.freshly.airquality.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vertyll.freshly.airquality.domain.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.AirQualityProvider;
import com.vertyll.freshly.airquality.domain.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.Station;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
class GiosAirQualityAdapter implements AirQualityProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GiosAirQualityAdapter(@Value("${gios.api-url}") String giosApiUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(giosApiUrl)
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
            log.error("Błąd parsowania JSON z GIOŚ", e);
            return List.of();
        }

        // Logowanie struktury odpowiedzi dla celów debugowania
        log.debug("GIOŚ Stations response: {}", root);

        // Próba znalezienia listy pod popularnymi kluczami w API GIOŚ
        JsonNode listNode = root.findPath("Lista stacji pomiarowych");
        if (listNode.isMissingNode()) listNode = root.findPath("lista");
        if (listNode.isMissingNode()) listNode = root.findPath("list");
        if (listNode.isMissingNode()) listNode = root.findPath("data");

        if (!listNode.isArray()) {
            log.warn("Nie znaleziono listy stacji w odpowiedzi. Otrzymano: {}", root);
            return List.of();
        }

        try {
            List<GiosStationDto> dtos = objectMapper.readerForListOf(GiosStationDto.class).readValue(listNode);
            return dtos.stream()
                    .map(this::mapToStation)
                    .toList();
        } catch (Exception e) {
            log.error("Błąd podczas deserializacji listy stacji", e);
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
            log.debug("GIOŚ Index response for station {}: {}", stationId, root);

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
            return Optional.ofNullable(dto).map(d -> mapToDomain(d, stationId));
        } catch (Exception e) {
            log.error("Błąd parsowania indeksu jakości powietrza", e);
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
            log.debug("GIOŚ Sensors response for station {}: {}", stationId, root);

            JsonNode listNode = root.findPath("Lista stanowisk pomiarowych dla podanej stacji");
            if (listNode.isMissingNode()) listNode = root.findPath("Lista stanowisk pomiarowych");
            if (listNode.isMissingNode()) listNode = root.findPath("Lista stanowisk");
            if (listNode.isMissingNode()) listNode = root;

            if (listNode.isArray()) {
                return objectMapper.readerForListOf(GiosSensorDto.class).readValue(listNode);
            }
        } catch (Exception e) {
            log.error("Błąd pobierania sensorów dla stacji {}", stationId, e);
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
            log.debug("GIOŚ Data response for sensor {}: {}", sensorId, root);

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
                        .filter(r -> r != null)
                        .toList();
            }
        } catch (Exception e) {
            log.error("Błąd pobierania danych dla sensora {}", sensorId, e);
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
        LocalDateTime date = dto.stCalcDate() != null ? LocalDateTime.parse(dto.stCalcDate(), formatter) : LocalDateTime.now();

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

    // Internal DTOs matching GIOŚ JSON structure
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosStationDto(
            @JsonAlias("Identyfikator stacji") Integer id,
            @JsonAlias("Nazwa stacji") String stationName,
            @JsonAlias("WGS84 φ N") String gegrLat,
            @JsonAlias("WGS84 λ E") String gegrLon,
            @JsonAlias("Nazwa miasta") String cityName,
            @JsonAlias("Ulica") String addressStreet
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosAQIndexDto(
            @JsonAlias("Identyfikator stacji pomiarowej") Integer id,
            @JsonAlias({"Data obliczenia", "Data wykonania obliczeń indeksu"}) String stCalcDate,
            @JsonAlias({"Indeks ogólny", "Nazwa kategorii indeksu"}) String stIndexLevel,
            @JsonAlias({"Indeks SO2", "Nazwa kategorii indeksu dla wskażnika SO2"}) String so2IndexLevel,
            @JsonAlias({"Indeks NO2", "Nazwa kategorii indeksu dla wskażnika NO2"}) String no2IndexLevel,
            @JsonAlias({"Indeks PM10", "Nazwa kategorii indeksu dla wskażnika PM10"}) String pm10IndexLevel
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosSensorDto(
            @JsonAlias({"Identyfikator stanowiska", "id", "Identyfikator"}) Integer id,
            @JsonAlias({"Wskaźnik - kod", "paramCode", "Kod wskaźnika"}) String paramCode,
            @JsonAlias({"Wskaźnik", "Nazwa parametru", "paramName", "Wskaźnik - nazwa"}) String paramName
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosDataValueDto(
            @JsonAlias({"data", "date", "Data"}) String date,
            @JsonAlias({"wartość", "value", "Wartość"}) Double value
    ) {}
}