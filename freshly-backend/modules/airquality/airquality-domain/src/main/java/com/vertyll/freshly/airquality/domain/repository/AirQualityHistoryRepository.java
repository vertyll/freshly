package com.vertyll.freshly.airquality.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.vertyll.freshly.airquality.domain.model.AirQualityMeasurement;
import com.vertyll.freshly.airquality.domain.model.AnalysisWindow;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;

public interface AirQualityHistoryRepository {
    AirQualityMeasurement save(AirQualityMeasurement measurement);

    List<AirQualityMeasurement> saveAll(List<AirQualityMeasurement> measurements);

    Optional<AirQualityMeasurement> findLatestByStationId(int stationId);

    PageResult<AirQualityMeasurement> findByStation(int stationId, AnalysisWindow window, PageRequest page);

    boolean hasMeasurementSince(int stationId, Instant threshold);

    long deleteOlderThan(Instant threshold);
}
