package com.vertyll.freshly.airquality.application.service.command;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vertyll.freshly.airquality.application.dto.SyncReport;
import com.vertyll.freshly.airquality.application.port.inbound.command.AirQualitySyncUseCase;
import com.vertyll.freshly.airquality.application.port.outbound.AirQualityProviderPort;
import com.vertyll.freshly.airquality.domain.model.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.model.AirQualityMeasurement;
import com.vertyll.freshly.airquality.domain.model.Pollutant;
import com.vertyll.freshly.airquality.domain.model.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.model.Station;
import com.vertyll.freshly.airquality.domain.repository.AirQualityHistoryRepository;
import com.vertyll.freshly.airquality.domain.repository.StationCatalogue;
import com.vertyll.freshly.lang.logging.UseCaseLogger;

public class AirQualitySyncService implements AirQualitySyncUseCase {
    private static final Duration FRESHNESS_WINDOW = Duration.ofMinutes(50);

    private static final Duration RETENTION = Duration.ofDays(90);

    private final AirQualityProviderPort provider;
    private final StationCatalogue stations;
    private final AirQualityHistoryRepository history;
    private final UseCaseLogger logger;

    public AirQualitySyncService(
        AirQualityProviderPort provider,
        StationCatalogue stations,
        AirQualityHistoryRepository history,
        UseCaseLogger logger
    ) {
        this.provider = provider;
        this.stations = stations;
        this.history = history;
        this.logger = logger;
    }

    @Override
    public SyncReport syncAll() {
        Instant started = Instant.now();
        List<Station> all = stations.findAll();

        int synced = 0;
        int skipped = 0;
        int failed = 0;

        Instant freshnessThreshold = started.minus(FRESHNESS_WINDOW);

        for (Station station : all) {
            switch (attempt(station, freshnessThreshold)) {
                case SYNCED -> synced++;
                case SKIPPED -> skipped++;
                case FAILED -> failed++;
            }
        }

        SyncReport report =
                new SyncReport(all.size(), synced, skipped, failed, Duration.between(started, Instant.now()));

        if (report.completelyFailed()) {
            logger.error("Sync touched {} stations and stored none", all.size());
        } else {
            logger.info("Sync stored {}, skipped {}, failed {}", synced, skipped, failed);
        }

        return report;
    }

    @Override
    public long purgeExpired() {
        long removed = history.deleteOlderThan(Instant.now().minus(RETENTION));
        logger.info("Purged {} measurements older than {} days", removed, RETENTION.toDays());
        return removed;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private Outcome attempt(Station station, Instant freshnessThreshold) {
        try {
            if (history.hasMeasurementSince(station.id(), freshnessThreshold)) {
                return Outcome.SKIPPED;
            }

            Optional<AirQualityIndex> index = provider.findCurrentIndex(station.id());
            if (index.isEmpty()) {
                return Outcome.SKIPPED;
            }

            Map<Pollutant, Double> readings = latestReadings(station.id());

            history.save(AirQualityMeasurement.record(station, index.get(), readings, index.get().calculatedAt()));

            return Outcome.SYNCED;

        } catch (RuntimeException e) {
            logger.warn("Could not sync station {} ({})", station.id(), station.name(), e);
            return Outcome.FAILED;
        }
    }

    private Map<Pollutant, Double> latestReadings(int stationId) {
        Map<Pollutant, Double> readings = new EnumMap<>(Pollutant.class);

        for (SensorMeasurement sensor : provider.findSensorMeasurements(stationId)) {
            sensor.readings()
                .stream()
                .filter(reading -> reading.value() != null)
                .max(Comparator.comparing(SensorMeasurement.Reading::measuredAt))
                .map(SensorMeasurement.Reading::value)
                .ifPresent(value -> readings.put(sensor.pollutant(), value));
        }

        return readings;
    }

    private enum Outcome {
        SYNCED,
        SKIPPED,
        FAILED
    }
}
