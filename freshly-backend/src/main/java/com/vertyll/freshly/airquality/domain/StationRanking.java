package com.vertyll.freshly.airquality.domain;

/**
 * Value object representing a station in a ranking
 */
public record StationRanking(
        int rank,
        Station station,
        Double averageScore,
        String dominantQualityLevel,
        int measurementCount
) {
    public boolean hasGoodAirQuality() {
        return "Bardzo dobry".equalsIgnoreCase(dominantQualityLevel) || "Dobry".equalsIgnoreCase(dominantQualityLevel);
    }
}
