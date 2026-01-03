package com.vertyll.freshly.airquality.domain;

import java.time.LocalDateTime;
import java.util.List;

public record SensorMeasurement(
        int sensorId,
        String paramCode, // np. "PM10", "NO2"
        String paramName, // np. "pył zawieszony PM10"
        List<Reading> readings
) {
    public record Reading(LocalDateTime date, Double value) {}
}