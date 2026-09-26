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

@Document(collection = "airquality_measurement")
@CompoundIndex(name = "uk_station_measured_at", def = "{'station_id': 1, 'measured_at': -1}", unique = true)
public record AirQualityMeasurementDocument(
    @Id UUID id,
    @Field("station_id") @Indexed int stationId,
    @Field("station_name") String stationName,
    @Field("measured_at") Instant measuredAt,
    @Field("recorded_at") Instant recordedAt,
    @Field("overall_level") @Nullable String overallLevel,
    @Field("index_levels") Map<String, String> indexLevels,
    @Field("readings") Map<String, Double> readings
) {
}
