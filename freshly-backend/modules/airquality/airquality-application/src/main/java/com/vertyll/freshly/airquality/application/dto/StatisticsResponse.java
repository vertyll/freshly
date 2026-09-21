package com.vertyll.freshly.airquality.application.dto;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.airquality.domain.model.AirQualityStatistics;

public record StatisticsResponse(
    int stationId,
    String stationName,
    Instant periodStart,
    Instant periodEnd,
    int measurementCount,
    @Nullable String dominantQualityLevel,
    Map<String, PollutantSummaryResponse> pollutants,
    Map<String, Integer> levelDistribution
) {

    public StatisticsResponse {
        pollutants = Map.copyOf(pollutants);
        levelDistribution = Map.copyOf(levelDistribution);
    }

    public record PollutantSummaryResponse(
        @Nullable Double average,
        @Nullable Double minimum,
        @Nullable Double maximum
    ) {
    }

    public static StatisticsResponse from(AirQualityStatistics statistics) {
        Map<String, PollutantSummaryResponse> pollutants = new LinkedHashMap<>();
        statistics.pollutants()
            .forEach(
                (pollutant, summary) -> pollutants.put(
                    pollutant.name(),
                    new PollutantSummaryResponse(summary.average(), summary.minimum(), summary.maximum())
                )
            );

        Map<String, Integer> distribution = new LinkedHashMap<>();
        statistics.levelDistribution().forEach((level, count) -> distribution.put(level.name(), count));

        return new StatisticsResponse(
            statistics.stationId(),
            statistics.stationName(),
            statistics.window().from(),
            statistics.window().to(),
            statistics.measurementCount(),
            statistics.dominantLevel().map(Enum::name).orElse(null),
            pollutants,
            distribution
        );
    }
}
