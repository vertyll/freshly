package com.vertyll.freshly.airquality.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class StationDistanceTest {

    @Test
    @DisplayName("Should create station distance successfully")
    void shouldCreateStationDistanceSuccessfully() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        double distance = 5.5;

        // When
        StationDistance stationDistance = new StationDistance(station, distance);

        // Then
        assertThat(stationDistance.station()).isEqualTo(station);
        assertThat(stationDistance.distanceInKm()).isEqualTo(distance);
    }

    @Test
    @DisplayName("Should create station distance with zero distance")
    void shouldCreateStationDistanceWithZeroDistance() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        double distance = 0.0;

        // When
        StationDistance stationDistance = new StationDistance(station, distance);

        // Then
        assertThat(stationDistance.distanceInKm()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should throw exception when distance is negative")
    void shouldThrowExceptionWhenDistanceIsNegative() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        double negativeDistance = -1.0;

        // When & Then
        assertThatThrownBy(() -> new StationDistance(station, negativeDistance))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Distance cannot be negative");
    }

    @Test
    @DisplayName("Should create station distance with large distance")
    void shouldCreateStationDistanceWithLargeDistance() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        double largeDistance = 1000.0;

        // When
        StationDistance stationDistance = new StationDistance(station, largeDistance);

        // Then
        assertThat(stationDistance.distanceInKm()).isEqualTo(largeDistance);
    }

    @Test
    @DisplayName("Should create station distance with decimal precision")
    void shouldCreateStationDistanceWithDecimalPrecision() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        double preciseDistance = 5.123456789;

        // When
        StationDistance stationDistance = new StationDistance(station, preciseDistance);

        // Then
        assertThat(stationDistance.distanceInKm()).isEqualTo(preciseDistance);
    }
}
