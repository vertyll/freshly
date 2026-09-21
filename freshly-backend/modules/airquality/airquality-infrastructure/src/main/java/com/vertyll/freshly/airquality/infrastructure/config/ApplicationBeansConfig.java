package com.vertyll.freshly.airquality.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vertyll.freshly.airquality.application.port.inbound.command.AirQualitySyncUseCase;
import com.vertyll.freshly.airquality.application.port.inbound.query.AirQualityQueryUseCase;
import com.vertyll.freshly.airquality.application.port.outbound.AirQualityAnalyticsPort;
import com.vertyll.freshly.airquality.application.port.outbound.AirQualityProviderPort;
import com.vertyll.freshly.airquality.application.service.command.AirQualitySyncService;
import com.vertyll.freshly.airquality.application.service.query.AirQualityQueryService;
import com.vertyll.freshly.airquality.domain.repository.AirQualityHistoryRepository;
import com.vertyll.freshly.airquality.domain.repository.StationCatalogue;
import com.vertyll.freshly.infra.logging.Slf4jUseCaseLogger;
import com.vertyll.freshly.infra.transaction.TransactionalUseCaseFactory;

@Configuration
public class ApplicationBeansConfig {
    @Bean
    AirQualityQueryUseCase airQualityQueryUseCase(
        TransactionalUseCaseFactory transactions,
        AirQualityHistoryRepository history,
        AirQualityAnalyticsPort analytics,
        StationCatalogue stations
    ) {
        return transactions
            .readOnly(AirQualityQueryUseCase.class, new AirQualityQueryService(history, analytics, stations));
    }

    @Bean
    AirQualitySyncUseCase airQualitySyncUseCase(
        AirQualityProviderPort provider,
        StationCatalogue stations,
        AirQualityHistoryRepository history
    ) {
        return new AirQualitySyncService(
            provider,
            stations,
            history,
            new Slf4jUseCaseLogger(AirQualitySyncService.class)
        );
    }
}
