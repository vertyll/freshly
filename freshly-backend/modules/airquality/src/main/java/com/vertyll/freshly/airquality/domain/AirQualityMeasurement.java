package com.vertyll.freshly.airquality.domain;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.jspecify.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain aggregate representing historical air quality measurement. This allows us to store and
 * analyze air quality data over time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AirQualityMeasurement {

    @Nullable private String id; // MongoDB ID
    private int stationId;
    @Nullable private String stationName;
    @Nullable private LocalDateTime measurementDate;

    // Index data
    @Nullable private AirQualityLevel overallIndexLevel;
    @Nullable private AirQualityLevel so2IndexLevel;
    @Nullable private AirQualityLevel no2IndexLevel;
    @Nullable private AirQualityLevel pm10IndexLevel;
    @Nullable private AirQualityLevel pm25IndexLevel;

    // Sensor readings
    @Nullable private Double pm10Value;
    @Nullable private Double pm25Value;
    @Nullable private Double so2Value;
    @Nullable private Double no2Value;
    @Nullable private Double coValue;
    @Nullable private Double o3Value;

    @Nullable private LocalDateTime createdAt;

    /** Factory method to create measurement from index and sensor data */
    public static AirQualityMeasurement create(
            int stationId,
            String stationName,
            AirQualityIndex index,
            java.util.Map<String, Double> sensorValues) {
        AirQualityMeasurement measurement = new AirQualityMeasurement();
        measurement.stationId = stationId;
        measurement.stationName = stationName;
        measurement.measurementDate = index.calculationDate();
        // Convert Polish names from GIOS API to enum
        measurement.overallIndexLevel = AirQualityLevel.fromPolishName(index.stIndexLevel());
        measurement.so2IndexLevel = AirQualityLevel.fromPolishName(index.so2IndexLevel());
        measurement.no2IndexLevel = AirQualityLevel.fromPolishName(index.no2IndexLevel());
        measurement.pm10IndexLevel = AirQualityLevel.fromPolishName(index.pm10IndexLevel());

        measurement.pm10Value = sensorValues.get("PM10");
        measurement.pm25Value = sensorValues.get("PM2.5");
        measurement.so2Value = sensorValues.get("SO2");
        measurement.no2Value = sensorValues.get("NO2");
        measurement.coValue = sensorValues.get("CO");
        measurement.o3Value = sensorValues.get("O3");

        measurement.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        return measurement;
    }

    public boolean hasGoodAirQuality() {
        return overallIndexLevel != null && overallIndexLevel.isGood();
    }
}
