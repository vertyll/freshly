package com.vertyll.freshly.airquality.api.dto;

import java.time.LocalDateTime;

public record AirQualityIndexResponseDto(
        int stationId,
        LocalDateTime calculationDate,
        String overallIndex,
        String so2Index,
        String no2Index,
        String pm10Index
) {
}
