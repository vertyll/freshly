package com.vertyll.freshly.airquality.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AirQualityStatisticsTest {
    private static final AnalysisWindow WINDOW =
            AnalysisWindow.between(Instant.now().minus(Duration.ofDays(7)), Instant.now());

    @Test
    @DisplayName("no measurements means no dominant level, not the worst one")
    void emptyMeansUnknown() {
        AirQualityStatistics statistics =
                new AirQualityStatistics(1, "Test", WINDOW, 0, Map.of(), AirQualityStatistics.emptyDistribution());

        assertThat(statistics.dominantLevel()).isEmpty();
    }

    @Test
    @DisplayName("picks the most frequent level")
    void picksMostFrequent() {
        Map<AirQualityLevel, Integer> distribution = new EnumMap<>(AirQualityLevel.class);
        distribution.put(AirQualityLevel.GOOD, 3);
        distribution.put(AirQualityLevel.MODERATE, 8);
        distribution.put(AirQualityLevel.BAD, 1);

        AirQualityStatistics statistics = new AirQualityStatistics(1, "Test", WINDOW, 12, Map.of(), distribution);

        assertThat(statistics.dominantLevel()).contains(AirQualityLevel.MODERATE);
    }

    @Test
    @DisplayName("a tie resolves towards the worse level")
    void tieGoesToWorse() {
        Map<AirQualityLevel, Integer> distribution = new EnumMap<>(AirQualityLevel.class);
        distribution.put(AirQualityLevel.GOOD, 5);
        distribution.put(AirQualityLevel.BAD, 5);

        AirQualityStatistics statistics = new AirQualityStatistics(1, "Test", WINDOW, 10, Map.of(), distribution);

        assertThat(statistics.dominantLevel()).contains(AirQualityLevel.BAD);
    }

    @Test
    @DisplayName("a pollutant with no sensor is absent, not a summary of nulls")
    void absentPollutantIsAbsent() {
        AirQualityStatistics statistics = new AirQualityStatistics(
            1,
            "Test",
            WINDOW,
            5,
            Map.of(Pollutant.PM10, new AirQualityStatistics.PollutantSummary(21.0, 4.0, 60.0)),
            AirQualityStatistics.emptyDistribution()
        );

        assertThat(statistics.summaryFor(Pollutant.PM10)).isPresent();
        assertThat(statistics.summaryFor(Pollutant.CO)).isEmpty();
    }
}
