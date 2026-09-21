package com.vertyll.freshly.airquality.domain.model;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public record Station(int id, String name, @Nullable String city, @Nullable String address, Coordinates coordinates) {
    public Station {
        requireNonNull(name, "Station name cannot be null");
        requireNonNull(coordinates, "Station coordinates cannot be null");
    }

    public double distanceTo(Coordinates point) {
        return coordinates.distanceTo(point);
    }
}
