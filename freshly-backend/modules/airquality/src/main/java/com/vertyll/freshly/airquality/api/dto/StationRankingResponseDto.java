package com.vertyll.freshly.airquality.api.dto;

public record StationRankingResponseDto(
        int rank,
        StationResponseDto station,
        Double averageScore,
        String dominantQualityLevel,
        int measurementCount
) {
}
