package com.vertyll.freshly.airquality.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.airquality.application.dto.SyncReport;
import com.vertyll.freshly.airquality.application.port.inbound.command.AirQualitySyncUseCase;
import com.vertyll.freshly.airquality.infrastructure.persistence.adapter.StationCache;
import com.vertyll.freshly.lang.error.DomainException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(name = "application.airquality.sync.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class AirQualitySyncScheduler {
    private static final String HOURLY_AT_FORTY_PAST = "0 40 * * * *";
    private static final String DAILY_AT_THREE = "0 0 3 * * *";
    private static final String DAILY_AT_HALF_THREE = "0 30 3 * * *";

    private final AirQualitySyncUseCase sync;
    private final StationCache stations;

    @Scheduled(cron = HOURLY_AT_FORTY_PAST)
    public void synchronise() {
        SyncReport report;
        try {
            report = sync.syncAll();
        } catch (DomainException e) {
            log.error("Air quality sync could not reach the provider", e);
            return;
        }

        if (report.completelyFailed()) {
            log.error(
                "Air quality sync stored nothing across {} stations in {}",
                report.stationsConsidered(),
                report.took()
            );
        }
    }

    @Scheduled(cron = DAILY_AT_THREE)
    public void purge() {
        sync.purgeExpired();
    }

    @Scheduled(cron = DAILY_AT_HALF_THREE)
    public void refreshStations() {
        stations.refresh();
    }
}
