package com.vertyll.freshly.airquality.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StationRankingTest {

    @Test
    @DisplayName("Should create station ranking successfully")
    void shouldCreateStationRankingSuccessfully() {
        // Given
        int rank = 1;
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        double averageScore = 25.5;
        AirQualityLevel dominantLevel = AirQualityLevel.GOOD;
        int measurementCount = 100;

        // When
        StationRanking ranking =
                new StationRanking(rank, station, averageScore, dominantLevel, measurementCount);

        // Then
        assertThat(ranking.rank()).isEqualTo(rank);
        assertThat(ranking.station()).isEqualTo(station);
        assertThat(ranking.averageScore()).isEqualTo(averageScore);
        assertThat(ranking.dominantQualityLevel()).isEqualTo(dominantLevel);
        assertThat(ranking.measurementCount()).isEqualTo(measurementCount);
    }

    @Test
    @DisplayName("Should detect good air quality for VERY_GOOD level")
    void shouldDetectGoodAirQualityForVeryGoodLevel() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        StationRanking ranking =
                new StationRanking(1, station, 15.0, AirQualityLevel.VERY_GOOD, 100);

        // When
        boolean result = ranking.hasGoodAirQuality();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should detect good air quality for GOOD level")
    void shouldDetectGoodAirQualityForGoodLevel() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        StationRanking ranking = new StationRanking(1, station, 25.0, AirQualityLevel.GOOD, 100);

        // When
        boolean result = ranking.hasGoodAirQuality();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should detect bad air quality for MODERATE level")
    void shouldDetectBadAirQualityForModerateLevel() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        StationRanking ranking =
                new StationRanking(1, station, 45.0, AirQualityLevel.MODERATE, 100);

        // When
        boolean result = ranking.hasGoodAirQuality();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should detect bad air quality for BAD level")
    void shouldDetectBadAirQualityForBadLevel() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        StationRanking ranking = new StationRanking(1, station, 75.0, AirQualityLevel.BAD, 100);

        // When
        boolean result = ranking.hasGoodAirQuality();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should detect bad air quality for VERY_BAD level")
    void shouldDetectBadAirQualityForVeryBadLevel() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        StationRanking ranking =
                new StationRanking(1, station, 120.0, AirQualityLevel.VERY_BAD, 100);

        // When
        boolean result = ranking.hasGoodAirQuality();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should create ranking with zero measurements")
    void shouldCreateRankingWithZeroMeasurements() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        StationRanking ranking = new StationRanking(1, station, 0.0, AirQualityLevel.VERY_GOOD, 0);

        // Then
        assertThat(ranking.measurementCount()).isZero();
        assertThat(ranking.averageScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should create ranking with null dominant quality level")
    void shouldCreateRankingWithNullDominantQualityLevel() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        StationRanking ranking = new StationRanking(1, station, 25.5, null, 100);

        // Then
        assertThat(ranking.dominantQualityLevel()).isNull();
        assertThat(ranking.hasGoodAirQuality()).isFalse();
    }

    @Test
    @DisplayName("Should create ranking with high average score")
    void shouldCreateRankingWithHighAverageScore() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        double highScore = 150.5;
        StationRanking ranking =
                new StationRanking(1, station, highScore, AirQualityLevel.VERY_BAD, 100);

        // Then
        assertThat(ranking.averageScore()).isEqualTo(highScore);
        assertThat(ranking.hasGoodAirQuality()).isFalse();
    }
}
