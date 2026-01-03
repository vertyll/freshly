package com.vertyll.freshly.airquality.domain;

import java.time.LocalDateTime;

public record AirQualityIndex(
        int stationId,
        LocalDateTime calculationDate,
        String stIndexLevel, // Ogólny indeks jakości
        String so2IndexLevel,
        String no2IndexLevel,
        String pm10IndexLevel
) {}