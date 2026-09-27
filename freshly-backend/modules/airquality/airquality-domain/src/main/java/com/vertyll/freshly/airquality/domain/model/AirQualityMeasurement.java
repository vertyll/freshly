package com.vertyll.freshly.airquality.domain.model;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class AirQualityMeasurement {
    private static final String STATION_NAME_NULL = "Station name cannot be null";
    private static final String MEASURED_AT_NULL = "Measurement timestamp cannot be null";

    private final UUID id;
    private final int stationId;
    private final String stationName;
    private final Instant measuredAt;
    private final Instant recordedAt;

    private final Map<Pollutant, AirQualityLevel> indexLevels;
    private final Map<Pollutant, Double> readings;
    @Nullable private final AirQualityLevel overallLevel;

    @SuppressWarnings("java:S107")
    private AirQualityMeasurement(
        UUID id,
        int stationId,
        String stationName,
        Instant measuredAt,
        Instant recordedAt,
        @Nullable AirQualityLevel overallLevel,
        Map<Pollutant, AirQualityLevel> indexLevels,
        Map<Pollutant, Double> readings
    ) {
        this.id = requireNonNull(id, "Measurement id cannot be null");
        this.stationId = stationId;
        this.stationName = requireNonNull(stationName, STATION_NAME_NULL);
        this.measuredAt = requireNonNull(measuredAt, MEASURED_AT_NULL);
        this.recordedAt = requireNonNull(recordedAt);
        this.overallLevel = overallLevel;
        this.indexLevels = copyLevels(indexLevels);
        this.readings = copyReadings(readings);
    }

    public static AirQualityMeasurement of(
        Station station,
        AirQualityIndex index,
        Map<Pollutant, Double> readings,
        Instant measuredAt
    ) {
        Map<Pollutant, AirQualityLevel> levels = new EnumMap<>(Pollutant.class);
        putIfPresent(levels, Pollutant.SO2, index.so2());
        putIfPresent(levels, Pollutant.NO2, index.no2());
        putIfPresent(levels, Pollutant.PM10, index.pm10());
        putIfPresent(levels, Pollutant.PM25, index.pm25());
        putIfPresent(levels, Pollutant.O3, index.o3());

        return new AirQualityMeasurement(
            UUID.randomUUID(),
            station.id(),
            station.name(),
            measuredAt,
            Instant.now(),
            index.overall(),
            levels,
            readings
        );
    }

    @SuppressWarnings("java:S107")
    public static AirQualityMeasurement reconstitute(
        UUID id,
        int stationId,
        String stationName,
        Instant measuredAt,
        Instant recordedAt,
        @Nullable AirQualityLevel overallLevel,
        Map<Pollutant, AirQualityLevel> indexLevels,
        Map<Pollutant, Double> readings
    ) {
        return new AirQualityMeasurement(
            id,
            stationId,
            stationName,
            measuredAt,
            recordedAt,
            overallLevel,
            indexLevels,
            readings
        );
    }

    public Optional<Double> readingFor(Pollutant pollutant) {
        return Optional.ofNullable(readings.get(pollutant));
    }

    public UUID id() {
        return id;
    }

    public int stationId() {
        return stationId;
    }

    public String stationName() {
        return stationName;
    }

    public Instant measuredAt() {
        return measuredAt;
    }

    public Instant recordedAt() {
        return recordedAt;
    }

    @Nullable public AirQualityLevel overallLevel() {
        return overallLevel;
    }

    public Map<Pollutant, AirQualityLevel> indexLevels() {
        return indexLevels;
    }

    public Map<Pollutant, Double> readings() {
        return readings;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AirQualityMeasurement measurement && stationId == measurement.stationId
                && measuredAt.equals(measurement.measuredAt);
    }

    @Override
    public int hashCode() {
        return 31 * Integer.hashCode(stationId) + measuredAt.hashCode();
    }

    private static void putIfPresent(
        Map<Pollutant, AirQualityLevel> target,
        Pollutant pollutant,
        @Nullable AirQualityLevel level
    ) {
        if (level != null) {
            target.put(pollutant, level);
        }
    }

    private static Map<Pollutant, AirQualityLevel> copyLevels(Map<Pollutant, AirQualityLevel> source) {
        return source.isEmpty() ? Map.of() : Map.copyOf(source);
    }

    private static Map<Pollutant, Double> copyReadings(Map<Pollutant, Double> source) {
        return source.isEmpty() ? Map.of() : Map.copyOf(source);
    }
}
