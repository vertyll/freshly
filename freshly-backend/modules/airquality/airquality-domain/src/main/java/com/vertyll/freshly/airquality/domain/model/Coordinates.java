package com.vertyll.freshly.airquality.domain.model;

import java.util.Map;

import com.vertyll.freshly.airquality.domain.error.AirQualityError;
import com.vertyll.freshly.lang.error.DomainException;

public record Coordinates(double latitude, double longitude) {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private static final double MAX_LATITUDE = 90.0;
    private static final double MAX_LONGITUDE = 180.0;

    public Coordinates {
        if (Math.abs(latitude) > MAX_LATITUDE || Math.abs(longitude) > MAX_LONGITUDE) {
            throw new DomainException(
                AirQualityError.INVALID_COORDINATES,
                Map.of("latitude", latitude, "longitude", longitude)
            );
        }
    }

    public double distanceTo(Coordinates other) {
        double deltaLat = Math.toRadians(other.latitude - latitude);
        double deltaLon = Math.toRadians(other.longitude - longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) + Math.cos(Math.toRadians(latitude))
                * Math.cos(Math.toRadians(other.latitude)) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
