package com.vertyll.freshly.airquality.application;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(
        name = "application.airquality.sync.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AirQualitySyncService {

    private static final String SYNC_CRON = "0 5 * * * *";
    private static final int RECENT_MEASUREMENT_THRESHOLD_MINUTES = 50;
    private static final int OLD_DATA_THRESHOLD_DAYS = 90;

    private final AirQualityProvider airQualityProvider;
    private final AirQualityHistoryRepository historyRepository;
    private final ObjectProvider<AirQualitySyncService> selfProvider;

    /**
     * Synchronize data every hour (5 minutes after the hour to allow GIOŚ to update) Cron: 0 5 * *
     * * * = at 5 minutes past every hour
     */
    @Scheduled(cron = SYNC_CRON)
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

            // Clean up old data (older than 90 days)
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
        // Check if we already have recent data (within the last 50 minutes)
        LocalDateTime threshold =
                LocalDateTime.now(ZoneOffset.UTC)
                        .minusMinutes(RECENT_MEASUREMENT_THRESHOLD_MINUTES);
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

        // Fetch sensor measurements and extract the latest values and their measurement dates
        List<SensorMeasurement> measurements =
                airQualityProvider.findMeasurementsByStationId(station.id());
        SensorDataResult sensorData = extractLatestSensorValues(measurements);

        // Use the latest sensor measurement date if available, otherwise fallback to index date
        LocalDateTime measurementDate = sensorData.latestDate().orElse(index.calculationDate());

        // Create and save measurement
        AirQualityMeasurement measurement =
                AirQualityMeasurement.create(
                        station.id(), station.name(), index, sensorData.values(), measurementDate);

        historyRepository.save(measurement);
        log.debug(
                "Saved measurement for station {} at {}: {}",
                station.name(),
                measurementDate,
                index.stIndexLevel());
    }

    private record SensorDataResult(
            Map<String, Double> values, Optional<LocalDateTime> latestDate) {}

    @SuppressWarnings("PMD.UseConcurrentHashMap") // Local variable, no concurrent access
    private SensorDataResult extractLatestSensorValues(List<SensorMeasurement> measurements) {
        Map<String, Double> values = new HashMap<>();
        LocalDateTime latestDate = null;

        for (SensorMeasurement sensor : measurements) {
            if (sensor.readings().isEmpty()) continue;

            // Get the most recent reading (first in a list after filtering nulls)
            Optional<SensorMeasurement.Reading> latestReading =
                    sensor.readings().stream().filter(r -> r.value() != null).findFirst();

            if (latestReading.isPresent()) {
                SensorMeasurement.Reading reading = latestReading.get();
                values.put(sensor.paramCode(), reading.value());

                // Track the overall latest measurement date across all sensors
                if (latestDate == null || reading.date().isAfter(latestDate)) {
                    latestDate = reading.date();
                }
            }
        }

        return new SensorDataResult(values, Optional.ofNullable(latestDate));
    }

    /** Remove measurements older than 90 days to prevent database bloat */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void cleanupOldData() {
        LocalDateTime threshold =
                LocalDateTime.now(ZoneOffset.UTC).minusDays(OLD_DATA_THRESHOLD_DAYS);
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
