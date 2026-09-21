package com.vertyll.freshly.airquality.domain.model;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

public record SensorMeasurement(int sensorId, Pollutant pollutant, List<Reading> readings) {
    public SensorMeasurement {
        readings = List.copyOf(readings);
    }

    public record Reading(Instant measuredAt, @Nullable Double value) {
    }
}
