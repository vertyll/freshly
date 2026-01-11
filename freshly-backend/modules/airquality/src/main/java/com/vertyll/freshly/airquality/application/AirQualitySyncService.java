package com.vertyll.freshly.airquality.application;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.airquality.domain.*;

/**
 * Scheduled service that synchronizes air quality data from GIOŚ API to MongoDB. Runs periodically
 * to build historical data for charts and analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AirQualitySyncService {

    private final AirQualityProvider airQualityProvider;
    private final AirQualityHistoryRepository historyRepository;
    private final ObjectProvider<AirQualitySyncService> selfProvider;

    /**
     * Synchronize data every hour (5 minutes after the hour to allow GIOŚ to update) Cron: 0 5 * *
     * * * = at 5 minutes past every hour
     */
    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void syncAirQualityData() {
        log.info("Starting scheduled air quality data synchronization");

        try {
            List<Station> stations = airQualityProvider.findAllStations();
            log.info("Found {} stations to sync", stations.size());

            int successCount = 0;
            int failedCount = 0;

            for (Station station : stations) {
                if (syncStationDataSafely(station)) {
                    successCount++;
                } else {
                    failedCount++;
                }
            }

            log.info("Sync completed: {} successful, {} failed", successCount, failedCount);

            // Cleanup old data (older than 90 days)
            cleanupOldData();

        } catch (Exception e) {
            log.error("Error during air quality data synchronization", e);
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private boolean syncStationDataSafely(Station station) {
        try {
            syncStationData(station);
            return true;
        } catch (Exception e) {
            log.error("Failed to sync data for station {}: {}", station.id(), e.getMessage());
            return false;
        }
    }

    private void syncStationData(Station station) {
        // Check if we already have recent data (within last 50 minutes)
        LocalDateTime threshold = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(50);
        if (historyRepository.hasRecentMeasurement(station.id(), threshold)) {
            log.debug("Skipping station {} - has recent measurement", station.id());
            return;
        }

        // Fetch air quality index
        Optional<AirQualityIndex> indexOpt = airQualityProvider.findIndexByStationId(station.id());
        if (indexOpt.isEmpty()) {
            log.debug("No index data for station {}", station.id());
            return;
        }

        AirQualityIndex index = indexOpt.get();

        // Fetch sensor measurements and extract latest values
        List<SensorMeasurement> measurements =
                airQualityProvider.findMeasurementsByStationId(station.id());
        Map<String, Double> latestValues = extractLatestSensorValues(measurements);

        // Create and save measurement
        AirQualityMeasurement measurement =
                AirQualityMeasurement.create(station.id(), station.name(), index, latestValues);

        historyRepository.save(measurement);
        log.debug("Saved measurement for station {}: {}", station.name(), index.stIndexLevel());
    }

    @SuppressWarnings("PMD.UseConcurrentHashMap") // Local variable, no concurrent access
    private Map<String, Double> extractLatestSensorValues(List<SensorMeasurement> measurements) {
        Map<String, Double> values = new HashMap<>();

        for (SensorMeasurement sensor : measurements) {
            if (sensor.readings().isEmpty()) continue;

            // Get the most recent reading (first in list after filtering nulls)
            sensor.readings().stream()
                    .filter(r -> r.value() != null)
                    .findFirst()
                    .map(SensorMeasurement.Reading::value)
                    .ifPresent(latestValue -> values.put(sensor.paramCode(), latestValue));
        }

        return values;
    }

    /** Remove measurements older than 90 days to prevent database bloat */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void cleanupOldData() {
        LocalDateTime threshold = LocalDateTime.now(ZoneOffset.UTC).minusDays(90);
        try {
            historyRepository.deleteOlderThan(threshold);
            log.info("Cleaned up measurements older than {}", threshold);
        } catch (Exception e) {
            log.error("Error cleaning up old measurements", e);
        }
    }

    /** Manual sync trigger for testing or admin operations */
    public void triggerManualSync() {
        log.info("Manual sync triggered");
        selfProvider.getObject().syncAirQualityData();
    }
}
