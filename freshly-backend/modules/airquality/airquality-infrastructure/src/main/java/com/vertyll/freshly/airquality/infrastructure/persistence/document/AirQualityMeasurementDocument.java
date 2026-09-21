package com.vertyll.freshly.airquality.infrastructure.persistence.document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "airquality_measurement")
@CompoundIndex(name = "uk_station_measured_at", def = "{'station_id': 1, 'measured_at': -1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AirQualityMeasurementDocument {
    @Id
    @Field("id")
    private UUID id;

    @Field("station_id")
    @Indexed
    private int stationId;

    @Field("station_name")
    private String stationName;

    @Field("measured_at")
    private Instant measuredAt;

    @Field("recorded_at")
    private Instant recordedAt;

    @Field("overall_level")
    @Nullable private String overallLevel;

    @Field("index_levels")
    private Map<String, String> indexLevels;

    @Field("readings")
    private Map<String, Double> readings;
}
