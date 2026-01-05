package com.vertyll.freshly.airquality.infrastructure.persistence.mongo;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.airquality.domain.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MongoDB implementation of AirQualityHistoryRepository. Adapts Spring Data repository to domain
 * repository interface.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MongoAirQualityHistoryRepository implements AirQualityHistoryRepository {

    private static final String FIELD_STATION_NAME = "stationName";
    private static final String FIELD_PM10_AVG = "pm10Avg";
    private static final String FIELD_PM25_AVG = "pm25Avg";

    private final SpringDataAirQualityMeasurementRepository springDataRepository;
    private final AirQualityMeasurementMapper mapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public AirQualityMeasurement save(AirQualityMeasurement measurement) {
        log.debug("Saving air quality measurement for station {}", measurement.getStationId());
        AirQualityMeasurementDocument document = mapper.toDocument(measurement);
        AirQualityMeasurementDocument saved = springDataRepository.save(document);
        return mapper.toDomain(saved);
    }

    @Override
    public List<AirQualityMeasurement> saveAll(List<AirQualityMeasurement> measurements) {
        log.debug("Batch saving {} air quality measurements", measurements.size());
        List<AirQualityMeasurementDocument> documents =
                measurements.stream().map(mapper::toDocument).toList();
        return springDataRepository.saveAll(documents).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<AirQualityMeasurement> findLatestByStationId(int stationId) {
        return springDataRepository
                .findFirstByStationIdOrderByMeasurementDateDesc(stationId)
                .map(mapper::toDomain);
    }

    @Override
    public List<AirQualityMeasurement> findByStationIdAndDateRange(
            int stationId, LocalDateTime from, LocalDateTime to) {
        return springDataRepository
                .findByStationIdAndMeasurementDateBetweenOrderByMeasurementDateAsc(
                        stationId, from, to)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AirQualityMeasurement> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return springDataRepository
                .findByMeasurementDateBetweenOrderByMeasurementDateDesc(from, to)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean hasRecentMeasurement(int stationId, LocalDateTime threshold) {
        return springDataRepository.countByStationIdAndMeasurementDateAfter(stationId, threshold)
                > 0;
    }

    @Override
    public void deleteOlderThan(LocalDateTime threshold) {
        log.info("Deleting air quality measurements older than {}", threshold);
        springDataRepository.deleteByMeasurementDateBefore(threshold);
    }

    @Override
    @SuppressWarnings("PMD.NPathComplexity") // Complex statistics aggregation logic
    public Optional<AirQualityStatistics> calculateStatistics(
            int stationId, LocalDateTime from, LocalDateTime to) {
        List<AirQualityMeasurementDocument> measurements =
                springDataRepository
                        .findByStationIdAndMeasurementDateBetweenOrderByMeasurementDateAsc(
                                stationId, from, to);

        if (measurements.isEmpty()) {
            return Optional.empty();
        }

        String stationName = measurements.getFirst().getStationName();
        int count = measurements.size();

        // Calculate PM10 statistics
        DoubleSummaryStatistics pm10Stats =
                measurements.stream()
                        .map(AirQualityMeasurementDocument::getPm10Value)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .summaryStatistics();

        // Calculate PM2.5 statistics
        DoubleSummaryStatistics pm25Stats =
                measurements.stream()
                        .map(AirQualityMeasurementDocument::getPm25Value)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .summaryStatistics();

        // Calculate averages for other pollutants
        double so2Avg =
                measurements.stream()
                        .map(AirQualityMeasurementDocument::getSo2Value)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

        double no2Avg =
                measurements.stream()
                        .map(AirQualityMeasurementDocument::getNo2Value)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

        double coAvg =
                measurements.stream()
                        .map(AirQualityMeasurementDocument::getCoValue)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

        double o3Avg =
                measurements.stream()
                        .map(AirQualityMeasurementDocument::getO3Value)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

        // Count quality levels (stored as enum in MongoDB)
        Map<AirQualityLevel, Long> qualityCounts =
                measurements.stream()
                        .map(AirQualityMeasurementDocument::getOverallIndexLevel)
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(level -> level, Collectors.counting()));

        return Optional.of(
                new AirQualityStatistics(
                        stationId,
                        stationName,
                        from,
                        to,
                        count,
                        pm10Stats.getCount() > 0 ? pm10Stats.getAverage() : null,
                        pm10Stats.getCount() > 0 ? pm10Stats.getMin() : null,
                        pm10Stats.getCount() > 0 ? pm10Stats.getMax() : null,
                        pm25Stats.getCount() > 0 ? pm25Stats.getAverage() : null,
                        pm25Stats.getCount() > 0 ? pm25Stats.getMin() : null,
                        pm25Stats.getCount() > 0 ? pm25Stats.getMax() : null,
                        so2Avg > 0 ? so2Avg : null,
                        no2Avg > 0 ? no2Avg : null,
                        coAvg > 0 ? coAvg : null,
                        o3Avg > 0 ? o3Avg : null,
                        qualityCounts.getOrDefault(AirQualityLevel.VERY_GOOD, 0L).intValue(),
                        qualityCounts.getOrDefault(AirQualityLevel.GOOD, 0L).intValue(),
                        qualityCounts.getOrDefault(AirQualityLevel.MODERATE, 0L).intValue(),
                        qualityCounts.getOrDefault(AirQualityLevel.SUFFICIENT, 0L).intValue(),
                        qualityCounts.getOrDefault(AirQualityLevel.BAD, 0L).intValue(),
                        qualityCounts.getOrDefault(AirQualityLevel.VERY_BAD, 0L).intValue()));
    }

    @Override
    public List<StationRanking> getRanking(LocalDateTime from, LocalDateTime to, int limit) {
        // MongoDB aggregation to calculate average scores per station
        Aggregation aggregation =
                Aggregation.newAggregation(
                        Aggregation.match(Criteria.where("measurementDate").gte(from).lte(to)),
                        Aggregation.group("stationId")
                                .first(FIELD_STATION_NAME)
                                .as(FIELD_STATION_NAME)
                                .avg("pm10Value")
                                .as(FIELD_PM10_AVG)
                                .avg("pm25Value")
                                .as(FIELD_PM25_AVG)
                                .first("overallIndexLevel")
                                .as("dominantQuality")
                                .count()
                                .as("measurementCount"),
                        Aggregation.sort(
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Order.asc(
                                                FIELD_PM10_AVG),
                                        org.springframework.data.domain.Sort.Order.asc(
                                                FIELD_PM25_AVG))),
                        Aggregation.limit(limit));

        AggregationResults<?> rawResults =
                mongoTemplate.aggregate(aggregation, "air_quality_measurements", Map.class);

        return getStationRankings(rawResults);
    }

    @SuppressWarnings(
            "PMD.AvoidInstantiatingObjectsInLoops") // Domain objects must be created per iteration
    private static List<StationRanking> getStationRankings(AggregationResults<?> rawResults) {
        List<StationRanking> rankings = new ArrayList<>();
        int rank = 1;

        for (Object resultObj : rawResults.getMappedResults()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) resultObj;
            Integer stationId = (Integer) result.get("_id");
            String stationName = (String) result.get(FIELD_STATION_NAME);
            Double pm10Avg = (Double) result.get(FIELD_PM10_AVG);
            Double pm25Avg = (Double) result.get(FIELD_PM25_AVG);
            String dominantQuality = (String) result.get("dominantQuality");
            Integer measurementCount = (Integer) result.get("measurementCount");

            // Calculate average score (lower is better)
            Double avgScore = null;
            if (pm10Avg != null && pm25Avg != null) {
                avgScore = (pm10Avg + pm25Avg) / 2.0;
            } else if (pm10Avg != null) {
                avgScore = pm10Avg;
            } else if (pm25Avg != null) {
                avgScore = pm25Avg;
            }

            // Create Station object (simplified - in real scenario you'd fetch full station data)
            Station station = new Station(stationId, stationName, "", "", 0.0, 0.0);

            // Convert dominant quality String to enum (MongoDB returns enum name as String)
            AirQualityLevel dominantLevel = null;
            if (dominantQuality != null) {
                try {
                    dominantLevel = AirQualityLevel.valueOf(dominantQuality);
                } catch (IllegalArgumentException _) {
                    log.warn("Unknown air quality level from database: {}", dominantQuality);
                }
            }

            rankings.add(
                    new StationRanking(
                            rank,
                            station,
                            avgScore,
                            dominantLevel,
                            measurementCount != null ? measurementCount : 0));
            rank++;
        }
        return rankings;
    }

    @Override
    public List<AirQualityMeasurement> findByGeoBounds(
            double minLat,
            double maxLat,
            double minLon,
            double maxLon,
            LocalDateTime from,
            LocalDateTime to) {
        // This would require stations to be embedded in measurements or a join
        // For now, return empty list - can be enhanced later
        log.warn("findByGeoBounds not yet implemented");
        return List.of();
    }
}
