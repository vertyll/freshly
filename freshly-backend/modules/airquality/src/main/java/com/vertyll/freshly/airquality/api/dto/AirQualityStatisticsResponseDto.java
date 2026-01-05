package com.vertyll.freshly.airquality.api.dto;

import java.time.LocalDateTime;

public record AirQualityStatisticsResponseDto(
        int stationId,
        String stationName,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        int measurementCount,
        Pm10Statistics pm10,
        Pm25Statistics pm25,
        OtherPollutants otherPollutants,
        QualityDistribution qualityDistribution,
        String mostCommonQualityLevel) {
    public record Pm10Statistics(Double average, Double min, Double max) {}

    public record Pm25Statistics(Double average, Double min, Double max) {}

    public record OtherPollutants(Double so2Avg, Double no2Avg, Double coAvg, Double o3Avg) {}

    public record QualityDistribution(
            int veryGood, int good, int moderate, int sufficient, int bad, int veryBad) {}
}
