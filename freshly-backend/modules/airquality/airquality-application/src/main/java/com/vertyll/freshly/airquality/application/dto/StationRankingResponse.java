package com.vertyll.freshly.airquality.application.dto;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.airquality.domain.model.AirQualityLevel;
import com.vertyll.freshly.airquality.domain.model.StationRanking;

public record StationRankingResponse(
    int rank,
    int stationId,
    String stationName,
    @Nullable String city,
    double averageScore,
    @Nullable String dominantQualityLevel,
    int measurementCount
) {

    public static StationRankingResponse from(StationRanking ranking) {
        AirQualityLevel level = ranking.dominantQualityLevel();
        return new StationRankingResponse(
            ranking.rank(),
            ranking.station().id(),
            ranking.station().name(),
            ranking.station().city(),
            ranking.averageScore(),
            level == null ? null : level.name(),
            ranking.measurementCount()
        );
    }
}
