package com.vertyll.freshly.airquality.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for persisting air quality measurements. This allows us to build
 * historical data for charts and analysis.
 */
public interface AirQualityHistoryRepository {

    /** Save a measurement to the database */
    AirQualityMeasurement save(AirQualityMeasurement measurement);

    /** Save multiple measurements in batch */
    List<AirQualityMeasurement> saveAll(List<AirQualityMeasurement> measurements);

    /** Find latest measurement for a station */
    Optional<AirQualityMeasurement> findLatestByStationId(int stationId);

    /** Find measurements for a station within time range (for charts) */
    List<AirQualityMeasurement> findByStationIdAndDateRange(
            int stationId, LocalDateTime from, LocalDateTime to);

    /** Find all measurements within time range (for general statistics) */
    List<AirQualityMeasurement> findByDateRange(LocalDateTime from, LocalDateTime to);

    /** Check if we have recent data (within last hour) to avoid unnecessary API calls */
    boolean hasRecentMeasurement(int stationId, LocalDateTime threshold);

    /** Delete old measurements (for data retention policy) */
    void deleteOlderThan(LocalDateTime threshold);

    /** Calculate statistics for a station within time range */
    Optional<AirQualityStatistics> calculateStatistics(
            int stationId, LocalDateTime from, LocalDateTime to);

    /**
     * Get ranking of stations (best to worst) within time range
     *
     * @param limit Max number of stations to return
     */
    List<StationRanking> getRanking(LocalDateTime from, LocalDateTime to, int limit);

    /** Find measurements for stations within geographic bounds */
    List<AirQualityMeasurement> findByGeoBounds(
            double minLat,
            double maxLat,
            double minLon,
            double maxLon,
            LocalDateTime from,
            LocalDateTime to);
}
