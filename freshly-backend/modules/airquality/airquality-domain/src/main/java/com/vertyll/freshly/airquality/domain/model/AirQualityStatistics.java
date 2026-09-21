package com.vertyll.freshly.airquality.domain.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

public record AirQualityStatistics(
    int stationId,
    String stationName,
    AnalysisWindow window,
    int measurementCount,
    Map<Pollutant, PollutantSummary> pollutants,
    Map<AirQualityLevel, Integer> levelDistribution
) {
    public AirQualityStatistics {
        pollutants = Map.copyOf(pollutants);
        levelDistribution = Map.copyOf(levelDistribution);
    }

    public record PollutantSummary(@Nullable Double average, @Nullable Double minimum, @Nullable Double maximum) {
    }

    public Optional<PollutantSummary> summaryFor(Pollutant pollutant) {
        return Optional.ofNullable(pollutants.get(pollutant));
    }

    public Optional<AirQualityLevel> dominantLevel() {
        if (measurementCount == 0 || levelDistribution.isEmpty()) {
            return Optional.empty();
        }

        return levelDistribution.entrySet()
            .stream()
            .filter(entry -> entry.getValue() > 0)
            .max(
                Map.Entry.<AirQualityLevel, Integer>comparingByValue()
                    .thenComparingInt(entry -> entry.getKey().severity())
            )
            .map(Map.Entry::getKey);
    }

    public static Map<AirQualityLevel, Integer> emptyDistribution() {
        Map<AirQualityLevel, Integer> distribution = new EnumMap<>(AirQualityLevel.class);
        for (AirQualityLevel level : AirQualityLevel.values()) {
            distribution.put(level, 0);
        }
        return distribution;
    }
}
