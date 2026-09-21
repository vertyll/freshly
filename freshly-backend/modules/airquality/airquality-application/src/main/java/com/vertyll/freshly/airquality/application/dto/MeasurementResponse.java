package com.vertyll.freshly.airquality.application.dto;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.airquality.domain.model.AirQualityLevel;
import com.vertyll.freshly.airquality.domain.model.AirQualityMeasurement;

public record MeasurementResponse(
    int stationId,
    String stationName,
    Instant measuredAt,
    @Nullable String overallLevel,
    Map<String, String> indexLevels,
    Map<String, Double> readings
) {

    public MeasurementResponse {
        indexLevels = Map.copyOf(indexLevels);
        readings = Map.copyOf(readings);
    }

    public static MeasurementResponse from(AirQualityMeasurement measurement) {
        Map<String, String> levels = new LinkedHashMap<>();
        measurement.indexLevels().forEach((pollutant, level) -> levels.put(pollutant.name(), level.name()));

        Map<String, Double> readings = new LinkedHashMap<>();
        measurement.readings().forEach((pollutant, value) -> readings.put(pollutant.name(), value));

        AirQualityLevel overall = measurement.overallLevel();

        return new MeasurementResponse(
            measurement.stationId(),
            measurement.stationName(),
            measurement.measuredAt(),
            overall == null ? null : overall.name(),
            levels,
            readings
        );
    }
}
