package com.vertyll.freshly.airquality.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.vertyll.freshly.airquality.infrastructure.persistence.document.AirQualityMeasurementDocument;

public interface SpringDataAirQualityRepository extends MongoRepository<AirQualityMeasurementDocument, UUID> {

    Optional<AirQualityMeasurementDocument> findFirstByStationIdOrderByMeasuredAtDesc(int stationId);

    Page<AirQualityMeasurementDocument> findByStationIdAndMeasuredAtBetween(
        int stationId,
        Instant from,
        Instant to,
        Pageable pageable
    );

    boolean existsByStationIdAndMeasuredAtGreaterThanEqual(int stationId, Instant threshold);

    long deleteByMeasuredAtLessThan(Instant threshold);
}
