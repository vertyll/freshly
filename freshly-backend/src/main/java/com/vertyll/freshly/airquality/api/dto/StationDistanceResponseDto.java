package com.vertyll.freshly.airquality.api.dto;

public record StationDistanceResponseDto(
        StationResponseDto station,
        double distanceInKm
) {
}
