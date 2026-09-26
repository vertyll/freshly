package com.vertyll.freshly.airquality.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.airquality.domain.model.AirQualityLevel;
import com.vertyll.freshly.airquality.domain.model.AirQualityMeasurement;
import com.vertyll.freshly.airquality.domain.model.AnalysisWindow;
import com.vertyll.freshly.airquality.domain.model.Pollutant;
import com.vertyll.freshly.airquality.domain.repository.AirQualityHistoryRepository;
import com.vertyll.freshly.airquality.infrastructure.persistence.document.AirQualityMeasurementDocument;
import com.vertyll.freshly.airquality.infrastructure.persistence.repository.SpringDataAirQualityRepository;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AirQualityHistoryPersistenceAdapter implements AirQualityHistoryRepository {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "measured_at");

    private final SpringDataAirQualityRepository repository;

    @Override
    public AirQualityMeasurement save(AirQualityMeasurement measurement) {
        return toDomain(repository.save(toDocument(measurement)));
    }

    @Override
    public List<AirQualityMeasurement> saveAll(List<AirQualityMeasurement> measurements) {
        return repository.saveAll(measurements.stream().map(AirQualityHistoryPersistenceAdapter::toDocument).toList())
            .stream()
            .map(AirQualityHistoryPersistenceAdapter::toDomain)
            .toList();
    }

    @Override
    public Optional<AirQualityMeasurement> findLatestByStationId(int stationId) {
        return repository.findFirstByStationIdOrderByMeasuredAtDesc(stationId)
            .map(AirQualityHistoryPersistenceAdapter::toDomain);
    }

    @Override
    public PageResult<AirQualityMeasurement> findByStation(int stationId, AnalysisWindow window, PageRequest page) {
        Page<AirQualityMeasurementDocument> found = repository.findByStationIdAndMeasuredAtBetween(
            stationId,
            window.from(),
            window.to(),
            org.springframework.data.domain.PageRequest.of(page.page(), page.size(), NEWEST_FIRST)
        );

        return new PageResult<>(
            found.getContent().stream().map(AirQualityHistoryPersistenceAdapter::toDomain).toList(),
            page.page(),
            page.size(),
            found.getTotalElements()
        );
    }

    @Override
    public boolean hasMeasurementSince(int stationId, Instant threshold) {
        return repository.existsByStationIdAndMeasuredAtGreaterThanEqual(stationId, threshold);
    }

    @Override
    public long deleteOlderThan(Instant threshold) {
        return repository.deleteByMeasuredAtLessThan(threshold);
    }

    private static AirQualityMeasurementDocument toDocument(AirQualityMeasurement measurement) {
        Map<String, String> levels = new LinkedHashMap<>();
        measurement.indexLevels().forEach((pollutant, level) -> levels.put(pollutant.name(), level.name()));

        Map<String, Double> readings = new LinkedHashMap<>();
        measurement.readings().forEach((pollutant, value) -> readings.put(pollutant.name(), value));

        AirQualityLevel overall = measurement.overallLevel();

        return new AirQualityMeasurementDocument(
            measurement.id(),
            measurement.stationId(),
            measurement.stationName(),
            measurement.measuredAt(),
            measurement.recordedAt(),
            overall == null ? null : overall.name(),
            levels,
            readings
        );
    }

    private static AirQualityMeasurement toDomain(AirQualityMeasurementDocument document) {
        Map<Pollutant, AirQualityLevel> levels = new EnumMap<>(Pollutant.class);
        document.indexLevels()
            .forEach((pollutant, level) -> levels.put(Pollutant.valueOf(pollutant), AirQualityLevel.valueOf(level)));

        Map<Pollutant, Double> readings = new EnumMap<>(Pollutant.class);
        document.readings().forEach((pollutant, value) -> readings.put(Pollutant.valueOf(pollutant), value));

        String overall = document.overallLevel();

        return AirQualityMeasurement.reconstitute(
            document.id(),
            document.stationId(),
            document.stationName(),
            document.measuredAt(),
            document.recordedAt(),
            overall == null ? null : AirQualityLevel.valueOf(overall),
            levels,
            readings
        );
    }
}
