package com.vertyll.freshly.airquality.infrastructure.gios;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
final class GiosResponses {
    private GiosResponses() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StationPage(
        @JsonProperty("Lista stacji pomiarowych") @Nullable List<StationDto> stations,
        @JsonProperty("totalPages") int totalPages
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StationDto(
        @JsonProperty("Identyfikator stacji") int id,
        @JsonProperty("Nazwa stacji") String name,
        @JsonProperty("WGS84 φ N") @Nullable String latitude,
        @JsonProperty("WGS84 λ E") @Nullable String longitude,
        @JsonProperty("Nazwa miasta") @Nullable String city,
        @JsonProperty("Ulica") @Nullable String street
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SensorList(
        @JsonProperty("Lista stanowisk pomiarowych dla podanej stacji") @Nullable List<SensorDto> sensors
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SensorDto(
        @JsonProperty("Identyfikator stanowiska") int id,
        @JsonProperty("Wskaźnik - kod") String pollutantCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DataPage(@JsonProperty("Lista danych pomiarowych") @Nullable List<ValueDto> values) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ValueDto(@JsonProperty("Data") String date, @JsonProperty("Wartość") @Nullable Double value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IndexEnvelope(@JsonProperty("AqIndex") @Nullable IndexDto index) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IndexDto(
        @JsonProperty("Data wykonania obliczeń indeksu") @Nullable String calculatedAt,
        @JsonProperty("Wartość indeksu") @Nullable Integer overall,
        @JsonProperty("Wartość indeksu dla wskaźnika SO2") @Nullable Integer so2,
        @JsonProperty("Wartość indeksu dla wskaźnika NO2") @Nullable Integer no2,
        @JsonProperty("Wartość indeksu dla wskaźnika PM10") @Nullable Integer pm10,
        @JsonProperty("Wartość indeksu dla wskaźnika PM2.5") @Nullable Integer pm25,
        @JsonProperty("Wartość indeksu dla wskaźnika O3") @Nullable Integer o3
    ) {
    }
}
