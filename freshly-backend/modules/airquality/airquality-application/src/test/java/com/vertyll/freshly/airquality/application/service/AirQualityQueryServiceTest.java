package com.vertyll.freshly.airquality.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vertyll.freshly.airquality.application.dto.MeasurementResponse;
import com.vertyll.freshly.airquality.application.dto.NearbyStationResponse;
import com.vertyll.freshly.airquality.application.dto.StationRankingResponse;
import com.vertyll.freshly.airquality.application.dto.StatisticsResponse;
import com.vertyll.freshly.airquality.application.port.outbound.AirQualityAnalyticsPort;
import com.vertyll.freshly.airquality.application.service.query.AirQualityQueryService;
import com.vertyll.freshly.airquality.domain.error.AirQualityError;
import com.vertyll.freshly.airquality.domain.model.AirQualityLevel;
import com.vertyll.freshly.airquality.domain.model.AirQualityMeasurement;
import com.vertyll.freshly.airquality.domain.model.AirQualityStatistics;
import com.vertyll.freshly.airquality.domain.model.AnalysisWindow;
import com.vertyll.freshly.airquality.domain.model.Coordinates;
import com.vertyll.freshly.airquality.domain.model.Pollutant;
import com.vertyll.freshly.airquality.domain.model.RankingLimit;
import com.vertyll.freshly.airquality.domain.model.SearchRadius;
import com.vertyll.freshly.airquality.domain.model.Station;
import com.vertyll.freshly.airquality.domain.model.StationDistance;
import com.vertyll.freshly.airquality.domain.model.StationRanking;
import com.vertyll.freshly.airquality.domain.repository.AirQualityHistoryRepository;
import com.vertyll.freshly.airquality.domain.repository.StationCatalogue;
import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;
import com.vertyll.freshly.lang.page.PagedResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AirQualityQueryServiceTest {
    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    private static final AnalysisWindow WINDOW = AnalysisWindow.between(NOW.minusSeconds(3600), NOW);
    private static final Station STATION = new Station(1, "Kraków", "Kraków", null, new Coordinates(50.0, 20.0));
    private static final int UNKNOWN_STATION = 2;
    private static final AirQualityMeasurement MEASUREMENT = AirQualityMeasurement.reconstitute(
        UUID.randomUUID(),
        STATION.id(),
        STATION.name(),
        NOW,
        NOW,
        AirQualityLevel.GOOD,
        Map.of(Pollutant.PM10, AirQualityLevel.GOOD),
        Map.of(Pollutant.PM10, 12.5)
    );

    private final AirQualityQueryService service =
            new AirQualityQueryService(new History(), new Analytics(), new Catalogue());

    private static void assertRefused(Runnable action, DomainError error) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(DomainException.class, e -> assertThat(e.error()).isEqualTo(error));
    }

    @Test
    void theCurrentAirIsTheLatestMeasurement() {
        MeasurementResponse current = service.currentFor(STATION.id());

        assertThat(current.overallLevel()).isEqualTo("GOOD");
        assertThat(current.indexLevels()).containsEntry("PM10", "GOOD");
        assertThat(current.readings()).containsEntry("PM10", 12.5);
    }

    @Test
    void aStationWithoutDataHasNoCurrentAir() {
        assertRefused(() -> service.currentFor(UNKNOWN_STATION), AirQualityError.DATA_NOT_FOUND);
    }

    @Test
    void theHistoryOfAStationIsPaged() {
        PagedResponse<MeasurementResponse> history = service.historyFor(STATION.id(), WINDOW, new PageRequest(0, 10));

        assertThat(history.items()).hasSize(1);
        assertThat(history.pagination().total()).isEqualTo(1);
    }

    @Test
    void theHistoryOfAnUnknownStationIsRefused() {
        PageRequest page = new PageRequest(0, 10);

        assertRefused(() -> service.historyFor(UNKNOWN_STATION, WINDOW, page), AirQualityError.STATION_NOT_FOUND);
    }

    @Test
    void statisticsNameTheDominantLevel() {
        StatisticsResponse statistics = service.statisticsFor(STATION.id(), WINDOW);

        assertThat(statistics.dominantQualityLevel()).isEqualTo("BAD");
        assertThat(statistics.pollutants()).containsOnlyKeys("PM10");
        assertThat(statistics.levelDistribution()).containsEntry("BAD", 2);
        assertThat(statistics.periodStart()).isEqualTo(WINDOW.from());
    }

    @Test
    void statisticsOfAnUnknownStationAreRefused() {
        assertRefused(() -> service.statisticsFor(UNKNOWN_STATION, WINDOW), AirQualityError.STATION_NOT_FOUND);
    }

    @Test
    void aStationWithoutMeasurementsInTheWindowHasNoStatistics() {
        AirQualityQueryService empty = new AirQualityQueryService(new History(), new NoAnalytics(), new Catalogue());

        assertRefused(() -> empty.statisticsFor(STATION.id(), WINDOW), AirQualityError.DATA_NOT_FOUND);
    }

    @Test
    void theRankingNamesEachStation() {
        List<StationRankingResponse> ranking = service.ranking(WINDOW, RankingLimit.of(5));

        assertThat(ranking).containsExactly(new StationRankingResponse(1, 1, "Kraków", "Kraków", 12.5, "GOOD", 3));
    }

    @Test
    void aStationWithoutADominantLevelIsRankedWithoutOne() {
        AirQualityQueryService noLevels = new AirQualityQueryService(new History(), new NoAnalytics(), new Catalogue());

        assertThat(noLevels.ranking(WINDOW, RankingLimit.of(5)))
            .extracting(StationRankingResponse::dominantQualityLevel)
            .containsOnlyNulls();
    }

    @Test
    void nearbyStationsComeFromTheCatalogue() {
        List<NearbyStationResponse> nearby = service.stationsNear(new Coordinates(50.0, 20.0), new SearchRadius(10.0));

        assertThat(nearby).extracting(NearbyStationResponse::stationId).containsExactly(STATION.id());
    }

    private static final class History implements AirQualityHistoryRepository {
        @Override
        public AirQualityMeasurement save(AirQualityMeasurement measurement) {
            return measurement;
        }

        @Override
        public List<AirQualityMeasurement> saveAll(List<AirQualityMeasurement> measurements) {
            return measurements;
        }

        @Override
        public Optional<AirQualityMeasurement> findLatestByStationId(int stationId) {
            return stationId == STATION.id() ? Optional.of(MEASUREMENT) : Optional.empty();
        }

        @Override
        public PageResult<AirQualityMeasurement> findByStation(int stationId, AnalysisWindow window, PageRequest page) {
            return new PageResult<>(List.of(MEASUREMENT), page.page(), page.size(), 1);
        }

        @Override
        public boolean hasMeasurementSince(int stationId, Instant threshold) {
            return true;
        }

        @Override
        public long deleteOlderThan(Instant threshold) {
            return 0;
        }
    }

    private static final class Analytics implements AirQualityAnalyticsPort {
        @Override
        public Optional<AirQualityStatistics> statisticsFor(int stationId, AnalysisWindow window) {
            return Optional.of(
                new AirQualityStatistics(
                    stationId,
                    STATION.name(),
                    window,
                    3,
                    Map.of(Pollutant.PM10, new AirQualityStatistics.PollutantSummary(12.5, 1.0, 20.0)),
                    Map.of(AirQualityLevel.GOOD, 1, AirQualityLevel.BAD, 2)
                )
            );
        }

        @Override
        public List<StationRanking> ranking(AnalysisWindow window, RankingLimit limit) {
            return List.of(new StationRanking(1, STATION, 12.5, AirQualityLevel.GOOD, 3));
        }
    }

    private static final class NoAnalytics implements AirQualityAnalyticsPort {
        @Override
        public Optional<AirQualityStatistics> statisticsFor(int stationId, AnalysisWindow window) {
            return Optional.empty();
        }

        @Override
        public List<StationRanking> ranking(AnalysisWindow window, RankingLimit limit) {
            return List.of(new StationRanking(1, STATION, 12.5, null, 3));
        }
    }

    private static final class Catalogue implements StationCatalogue {
        @Override
        public Optional<Station> findById(int stationId) {
            return stationId == STATION.id() ? Optional.of(STATION) : Optional.empty();
        }

        @Override
        public List<Station> findAll() {
            return List.of(STATION);
        }

        @Override
        public List<StationDistance> findWithin(Coordinates centre, SearchRadius radius) {
            return List.of(new StationDistance(STATION, 1.0));
        }
    }
}
