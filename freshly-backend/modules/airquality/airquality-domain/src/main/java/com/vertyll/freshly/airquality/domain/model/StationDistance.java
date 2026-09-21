package com.vertyll.freshly.airquality.domain.model;

public record StationDistance(Station station, double distanceInKm) {

    private static final String NEGATIVE_DISTANCE = "Distance cannot be negative";

    public StationDistance {
        if (distanceInKm < 0) {
            throw new IllegalArgumentException(NEGATIVE_DISTANCE);
        }
    }
}
