package com.vertyll.freshly.airquality.application.dto;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.airquality.domain.model.Station;
import com.vertyll.freshly.airquality.domain.model.StationDistance;

public record NearbyStationResponse(
    int stationId,
    String name,
    @Nullable String city,
    @Nullable String address,
    double latitude,
    double longitude,
    double distanceInKm
) {

    public static NearbyStationResponse from(StationDistance stationDistance) {
        Station station = stationDistance.station();
        return new NearbyStationResponse(
            station.id(),
            station.name(),
            station.city(),
            station.address(),
            station.coordinates().latitude(),
            station.coordinates().longitude(),
            Math.round(stationDistance.distanceInKm() * 100.0) / 100.0
        );
    }
}
