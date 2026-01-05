package com.vertyll.freshly.airquality.domain;

import java.time.LocalDateTime;
import java.util.List;

public record SensorMeasurement(
        int sensorId,
        String paramCode, // e.g. "PM10", "NO2"
        String paramName, // e.g. "Pył zawieszony PM10"
        List<Reading> readings) {
    public record Reading(LocalDateTime date, Double value) {}
}
