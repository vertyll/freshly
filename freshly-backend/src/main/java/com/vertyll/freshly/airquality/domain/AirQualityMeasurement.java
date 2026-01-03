package com.vertyll.freshly.airquality.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Domain aggregate representing historical air quality measurement.
 * This allows us to store and analyze air quality data over time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AirQualityMeasurement {

    private String id; // MongoDB ID
    private int stationId;
    private String stationName;
    private LocalDateTime measurementDate;

    // Index data
    private String overallIndexLevel;
    private String so2IndexLevel;
    private String no2IndexLevel;
    private String pm10IndexLevel;
    private String pm25IndexLevel;

    // Sensor readings
    private Double pm10Value;
    private Double pm25Value;
    private Double so2Value;
    private Double no2Value;
    private Double coValue;
    private Double o3Value;

    private LocalDateTime createdAt;

    /**
     * Factory method to create measurement from index and sensor data
     */
    public static AirQualityMeasurement create(
            int stationId,
            String stationName,
            AirQualityIndex index,
            java.util.Map<String, Double> sensorValues
    ) {
        var measurement = new AirQualityMeasurement();
        measurement.stationId = stationId;
        measurement.stationName = stationName;
        measurement.measurementDate = index.calculationDate();
        measurement.overallIndexLevel = index.stIndexLevel();
        measurement.so2IndexLevel = index.so2IndexLevel();
        measurement.no2IndexLevel = index.no2IndexLevel();
        measurement.pm10IndexLevel = index.pm10IndexLevel();

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
        return "Bardzo dobry".equalsIgnoreCase(overallIndexLevel) || "Dobry".equalsIgnoreCase(overallIndexLevel);
    }
}
