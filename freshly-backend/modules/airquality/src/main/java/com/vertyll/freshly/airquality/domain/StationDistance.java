package com.vertyll.freshly.airquality.domain;

/**
 * Value object representing a station with its distance from a point
 */
public record StationDistance(
        Station station,
        double distanceInKm
) {
    public StationDistance {
        if (distanceInKm < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }
    }
}
