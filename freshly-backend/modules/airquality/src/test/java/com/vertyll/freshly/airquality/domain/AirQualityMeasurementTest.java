package com.vertyll.freshly.airquality.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AirQualityMeasurementTest {

    @Test
    @DisplayName("Should create measurement from index and sensor data")
    void shouldCreateMeasurementFromIndexAndSensorData() {
        // Given
        int stationId = 123;
        String stationName = "Station 1";
        LocalDateTime calculationDate = LocalDateTime.now(ZoneOffset.UTC);
        AirQualityIndex index =
                new AirQualityIndex(
                        stationId,
                        calculationDate,
                        "Dobry",
                        "Bardzo dobry",
                        "Umiarkowany",
                        "Dobry");

        Map<String, Double> sensorValues =
                Map.of(
                        "PM10", 25.5,
                        "PM2.5", 15.2,
                        "SO2", 8.5,
                        "NO2", 12.3,
                        "CO", 0.5,
                        "O3", 45.2);

        // When
        AirQualityMeasurement measurement =
                AirQualityMeasurement.create(stationId, stationName, index, sensorValues);

        // Then
        assertThat(measurement.getStationId()).isEqualTo(stationId);
        assertThat(measurement.getStationName()).isEqualTo(stationName);
        assertThat(measurement.getMeasurementDate()).isEqualTo(calculationDate);
        assertThat(measurement.getOverallIndexLevel()).isEqualTo(AirQualityLevel.GOOD);
        assertThat(measurement.getSo2IndexLevel()).isEqualTo(AirQualityLevel.VERY_GOOD);
        assertThat(measurement.getNo2IndexLevel()).isEqualTo(AirQualityLevel.MODERATE);
        assertThat(measurement.getPm10IndexLevel()).isEqualTo(AirQualityLevel.GOOD);
        assertThat(measurement.getPm10Value()).isEqualTo(25.5);
        assertThat(measurement.getPm25Value()).isEqualTo(15.2);
        assertThat(measurement.getSo2Value()).isEqualTo(8.5);
        assertThat(measurement.getNo2Value()).isEqualTo(12.3);
        assertThat(measurement.getCoValue()).isEqualTo(0.5);
        assertThat(measurement.getO3Value()).isEqualTo(45.2);
        assertThat(measurement.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create measurement with missing sensor values")
    void shouldCreateMeasurementWithMissingSensorValues() {
        // Given
        int stationId = 123;
        String stationName = "Station 1";
        AirQualityIndex index =
                new AirQualityIndex(
                        stationId,
                        LocalDateTime.now(ZoneOffset.UTC),
                        "Dobry",
                        "Dobry",
                        "Dobry",
                        "Dobry");

        Map<String, Double> sensorValues = Map.of("PM10", 25.5);
        // Missing other sensor values

        // When
        AirQualityMeasurement measurement =
                AirQualityMeasurement.create(stationId, stationName, index, sensorValues);

        // Then
        assertThat(measurement.getPm10Value()).isEqualTo(25.5);
        assertThat(measurement.getPm25Value()).isNull();
        assertThat(measurement.getSo2Value()).isNull();
        assertThat(measurement.getNo2Value()).isNull();
        assertThat(measurement.getCoValue()).isNull();
        assertThat(measurement.getO3Value()).isNull();
    }

    @Test
    @DisplayName("Should detect good air quality")
    void shouldDetectGoodAirQuality() {
        // Given
        AirQualityMeasurement measurement = new AirQualityMeasurement();
        measurement.setOverallIndexLevel(AirQualityLevel.GOOD);

        // When
        boolean result = measurement.hasGoodAirQuality();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should detect very good air quality")
    void shouldDetectVeryGoodAirQuality() {
        // Given
        AirQualityMeasurement measurement = new AirQualityMeasurement();
        measurement.setOverallIndexLevel(AirQualityLevel.VERY_GOOD);

        // When
        boolean result = measurement.hasGoodAirQuality();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should detect bad air quality")
    void shouldDetectBadAirQuality() {
        // Given
        AirQualityMeasurement measurement = new AirQualityMeasurement();
        measurement.setOverallIndexLevel(AirQualityLevel.BAD);

        // When
        boolean result = measurement.hasGoodAirQuality();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle null overall index level")
    @SuppressWarnings("NullAway")
    void shouldHandleNullOverallIndexLevel() {
        // Given
        AirQualityMeasurement measurement = new AirQualityMeasurement();
        measurement.setOverallIndexLevel(null);

        // When
        boolean result = measurement.hasGoodAirQuality();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should set all measurement properties correctly")
    void shouldSetAllMeasurementPropertiesCorrectly() {
        // Given
        AirQualityMeasurement measurement = new AirQualityMeasurement();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // When
        measurement.setId("test-id");
        measurement.setStationId(123);
        measurement.setStationName("Station 1");
        measurement.setMeasurementDate(now);
        measurement.setOverallIndexLevel(AirQualityLevel.GOOD);
        measurement.setSo2IndexLevel(AirQualityLevel.VERY_GOOD);
        measurement.setNo2IndexLevel(AirQualityLevel.MODERATE);
        measurement.setPm10IndexLevel(AirQualityLevel.GOOD);
        measurement.setPm25IndexLevel(AirQualityLevel.GOOD);
        measurement.setPm10Value(25.5);
        measurement.setPm25Value(15.2);
        measurement.setSo2Value(8.5);
        measurement.setNo2Value(12.3);
        measurement.setCoValue(0.5);
        measurement.setO3Value(45.2);
        measurement.setCreatedAt(now);

        // Then
        assertThat(measurement.getId()).isEqualTo("test-id");
        assertThat(measurement.getStationId()).isEqualTo(123);
        assertThat(measurement.getStationName()).isEqualTo("Station 1");
        assertThat(measurement.getMeasurementDate()).isEqualTo(now);
        assertThat(measurement.getOverallIndexLevel()).isEqualTo(AirQualityLevel.GOOD);
        assertThat(measurement.getSo2IndexLevel()).isEqualTo(AirQualityLevel.VERY_GOOD);
        assertThat(measurement.getNo2IndexLevel()).isEqualTo(AirQualityLevel.MODERATE);
        assertThat(measurement.getPm10IndexLevel()).isEqualTo(AirQualityLevel.GOOD);
        assertThat(measurement.getPm25IndexLevel()).isEqualTo(AirQualityLevel.GOOD);
        assertThat(measurement.getPm10Value()).isEqualTo(25.5);
        assertThat(measurement.getPm25Value()).isEqualTo(15.2);
        assertThat(measurement.getSo2Value()).isEqualTo(8.5);
        assertThat(measurement.getNo2Value()).isEqualTo(12.3);
        assertThat(measurement.getCoValue()).isEqualTo(0.5);
        assertThat(measurement.getO3Value()).isEqualTo(45.2);
        assertThat(measurement.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should create measurement with empty sensor values map")
    void shouldCreateMeasurementWithEmptySensorValuesMap() {
        // Given
        int stationId = 123;
        String stationName = "Station 1";
        AirQualityIndex index =
                new AirQualityIndex(
                        stationId,
                        LocalDateTime.now(ZoneOffset.UTC),
                        "Dobry",
                        "Dobry",
                        "Dobry",
                        "Dobry");

        Map<String, Double> emptySensorValues = Map.of();

        // When
        AirQualityMeasurement measurement =
                AirQualityMeasurement.create(stationId, stationName, index, emptySensorValues);

        // Then
        assertThat(measurement.getStationId()).isEqualTo(stationId);
        assertThat(measurement.getStationName()).isEqualTo(stationName);
        assertThat(measurement.getPm10Value()).isNull();
        assertThat(measurement.getPm25Value()).isNull();
        assertThat(measurement.getSo2Value()).isNull();
        assertThat(measurement.getNo2Value()).isNull();
        assertThat(measurement.getCoValue()).isNull();
        assertThat(measurement.getO3Value()).isNull();
    }
}
