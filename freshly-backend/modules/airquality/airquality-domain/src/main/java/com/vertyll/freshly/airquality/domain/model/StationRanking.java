package com.vertyll.freshly.airquality.domain.model;

import org.jspecify.annotations.Nullable;

public record StationRanking(
    int rank,
    Station station,
    double averageScore,
    @Nullable AirQualityLevel dominantQualityLevel,
    int measurementCount
) {
}
