package com.vertyll.freshly.airquality.infrastructure.persistence.mongo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/** Spring Data MongoDB repository for air quality measurements */
public interface SpringDataAirQualityMeasurementRepository
        extends MongoRepository<AirQualityMeasurementDocument, String> {

    @Query(value = "{ 'stationId': ?0 }", sort = "{ 'measurementDate': -1 }")
    Optional<AirQualityMeasurementDocument> findFirstByStationIdOrderByMeasurementDateDesc(
            Integer stationId);

    List<AirQualityMeasurementDocument>
            findByStationIdAndMeasurementDateBetweenOrderByMeasurementDateAsc(
                    Integer stationId, LocalDateTime from, LocalDateTime to);

    List<AirQualityMeasurementDocument> findByMeasurementDateBetweenOrderByMeasurementDateDesc(
            LocalDateTime from, LocalDateTime to);

    @Query(value = "{ 'stationId': ?0, 'measurementDate': { $gte: ?1 } }", count = true)
    long countByStationIdAndMeasurementDateAfter(Integer stationId, LocalDateTime threshold);

    void deleteByMeasurementDateBefore(LocalDateTime threshold);
}
