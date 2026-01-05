package com.vertyll.freshly.airquality.domain;

import java.time.LocalDateTime;

/** Value object representing air quality statistics for a station */
public record AirQualityStatistics(
        int stationId,
        String stationName,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        int measurementCount,

        // PM10 statistics
        Double pm10Avg,
        Double pm10Min,
        Double pm10Max,

        // PM2.5 statistics
        Double pm25Avg,
        Double pm25Min,
        Double pm25Max,

        // Other pollutants
        Double so2Avg,
        Double no2Avg,
        Double coAvg,
        Double o3Avg,

        // Quality index distribution
        int veryGoodCount,
        int goodCount,
        int moderateCount,
        int sufficientCount,
        int badCount,
        int veryBadCount) {
    public AirQualityLevel getMostCommonQualityLevel() {
        int max =
                Math.max(
                        veryGoodCount,
                        Math.max(
                                goodCount,
                                Math.max(
                                        moderateCount,
                                        Math.max(
                                                sufficientCount,
                                                Math.max(badCount, veryBadCount)))));

        if (max == veryGoodCount) return AirQualityLevel.VERY_GOOD;
        if (max == goodCount) return AirQualityLevel.GOOD;
        if (max == moderateCount) return AirQualityLevel.MODERATE;
        if (max == sufficientCount) return AirQualityLevel.SUFFICIENT;
        if (max == badCount) return AirQualityLevel.BAD;
        return AirQualityLevel.VERY_BAD;
    }
}
