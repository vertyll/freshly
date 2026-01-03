package com.vertyll.freshly.airquality.api.dto;

import java.time.LocalDateTime;

public record AirQualityMeasurementResponseDto(
        String id,
        int stationId,
        String stationName,
        LocalDateTime measurementDate,
        String overallIndexLevel,
        String so2IndexLevel,
        String no2IndexLevel,
        String pm10IndexLevel,
        String pm25IndexLevel,
        Double pm10Value,
        Double pm25Value,
        Double so2Value,
        Double no2Value,
        Double coValue,
        Double o3Value,
        LocalDateTime createdAt
) {
}
