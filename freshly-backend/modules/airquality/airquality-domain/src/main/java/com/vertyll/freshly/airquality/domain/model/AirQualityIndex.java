package com.vertyll.freshly.airquality.domain.model;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record AirQualityIndex(
    int stationId,
    Instant calculatedAt,
    @Nullable AirQualityLevel overall,
    @Nullable AirQualityLevel so2,
    @Nullable AirQualityLevel no2,
    @Nullable AirQualityLevel pm10,
    @Nullable AirQualityLevel pm25,
    @Nullable AirQualityLevel o3
) {
}
