package com.vertyll.freshly.airquality.infrastructure.persistence.mongo;

import com.vertyll.freshly.airquality.domain.AirQualityLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document for air quality measurements.
 */
@Document(collection = "air_quality_measurements")
@CompoundIndexes({
        @CompoundIndex(name = "station_date_idx", def = "{'stationId': 1, 'measurementDate': -1}"),
        @CompoundIndex(name = "date_idx", def = "{'measurementDate': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
class AirQualityMeasurementDocument {

    @Id
    private String id;
    private Integer stationId;
    private String stationName;
    private LocalDateTime measurementDate;

    // Index data - MongoDB stores enum as string (enum name)
    private AirQualityLevel overallIndexLevel;
    private AirQualityLevel so2IndexLevel;
    private AirQualityLevel no2IndexLevel;
    private AirQualityLevel pm10IndexLevel;
    private AirQualityLevel pm25IndexLevel;

    // Sensor readings
    private Double pm10Value;
    private Double pm25Value;
    private Double so2Value;
    private Double no2Value;
    private Double coValue;
    private Double o3Value;

    private LocalDateTime createdAt;
}
