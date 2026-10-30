package com.vertyll.freshly;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bson.UuidRepresentation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.testcontainers.mongodb.MongoDBContainer;

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
import com.vertyll.freshly.airquality.domain.repository.StationCatalogue;
import com.vertyll.freshly.airquality.infrastructure.persistence.adapter.AirQualityAnalyticsAdapter;
import com.vertyll.freshly.airquality.infrastructure.persistence.adapter.AirQualityHistoryPersistenceAdapter;
import com.vertyll.freshly.airquality.infrastructure.persistence.repository.SpringDataAirQualityRepository;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import static org.assertj.core.api.Assertions.assertThat;

class AirQualityPersistenceTest {
    private static final Instant NOON = Instant.parse("2026-03-01T12:00:00Z");
    private static final AnalysisWindow THE_DAY =
            AnalysisWindow.between(NOON.minus(Duration.ofHours(12)), NOON.plus(Duration.ofHours(12)));
    private static final Station KRAKOW = station(1, "Kraków");
    private static final Station WARSAW = station(2, "Warszawa");

    @SuppressWarnings("resource")
    private static final MongoDBContainer MONGO =
            new MongoDBContainer("mongo:9").withStartupTimeout(Duration.ofMinutes(2)).withStartupAttempts(3);

    private static MongoClient client;
    private static MongoTemplate template;
    private static AirQualityHistoryPersistenceAdapter history;
    private static AirQualityAnalyticsAdapter analytics;

    @BeforeAll
    static void startMongo() {
        MONGO.start();
        client = MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(MONGO.getConnectionString()))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build()
        );
        template = new MongoTemplate(client, "freshly");
        history = new AirQualityHistoryPersistenceAdapter(
            new MongoRepositoryFactory(template).getRepository(SpringDataAirQualityRepository.class)
        );
        analytics = new AirQualityAnalyticsAdapter(template, new Catalogue());
    }

    @AfterAll
    static void stopMongo() {
        client.close();
        MONGO.stop();
    }

    @BeforeEach
    void noMeasurements() {
        template.dropCollection("airquality_measurement");
    }

    private static Station station(int id, String name) {
        return new Station(id, name, name, null, new Coordinates(50.0, 20.0));
    }

    private static AirQualityMeasurement measurement(
        Station station,
        Instant at,
        AirQualityLevel level,
        Map<Pollutant, Double> readings
    ) {
        Map<Pollutant, AirQualityLevel> levels = new EnumMap<>(Pollutant.class);
        readings.keySet().forEach(pollutant -> levels.put(pollutant, level));
        return AirQualityMeasurement
            .reconstitute(UUID.randomUUID(), station.id(), station.name(), at, at, level, levels, readings);
    }

    private static AirQualityMeasurement pm10(Station station, Instant at, AirQualityLevel level, double value) {
        return measurement(station, at, level, Map.of(Pollutant.PM10, value));
    }

    private static AirQualityMeasurement unrated(Station station, Instant at, double value) {
        return AirQualityMeasurement.reconstitute(
            UUID.randomUUID(),
            station.id(),
            station.name(),
            at,
            at,
            null,
            Map.of(),
            Map.of(Pollutant.PM10, value)
        );
    }

    @Test
    void aMeasurementIsSavedAndReadBackWithItsReadingsAndLevels() {
        AirQualityMeasurement saved = history
            .save(measurement(KRAKOW, NOON, AirQualityLevel.GOOD, Map.of(Pollutant.PM10, 12.5, Pollutant.O3, 40.0)));

        AirQualityMeasurement found = history.findLatestByStationId(KRAKOW.id()).orElseThrow();

        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.overallLevel()).isEqualTo(AirQualityLevel.GOOD);
        assertThat(found.readings()).containsEntry(Pollutant.PM10, 12.5).containsEntry(Pollutant.O3, 40.0);
        assertThat(found.indexLevels()).containsEntry(Pollutant.O3, AirQualityLevel.GOOD);
    }

    @Test
    void aMeasurementWithoutALevelKeepsNone() {
        AirQualityMeasurement unrated = AirQualityMeasurement
            .reconstitute(UUID.randomUUID(), KRAKOW.id(), KRAKOW.name(), NOON, NOON, null, Map.of(), Map.of());

        history.save(unrated);

        assertThat(history.findLatestByStationId(KRAKOW.id()).orElseThrow().overallLevel()).isNull();
    }

    @Test
    void theLatestMeasurementIsTheNewestOfItsStation() {
        history.saveAll(
            List.of(
                pm10(KRAKOW, NOON, AirQualityLevel.GOOD, 1.0),
                pm10(KRAKOW, NOON.plusSeconds(60), AirQualityLevel.BAD, 2.0),
                pm10(WARSAW, NOON.plusSeconds(120), AirQualityLevel.GOOD, 3.0)
            )
        );

        assertThat(history.findLatestByStationId(KRAKOW.id()).orElseThrow().overallLevel())
            .isEqualTo(AirQualityLevel.BAD);
    }

    @Test
    void aStationWithoutMeasurementsHasNoLatestOne() {
        assertThat(history.findLatestByStationId(KRAKOW.id())).isEmpty();
    }

    @Test
    void historyIsPagedNewestFirstWithinTheWindow() {
        history.saveAll(
            List.of(
                pm10(KRAKOW, NOON, AirQualityLevel.GOOD, 1.0),
                pm10(KRAKOW, NOON.plusSeconds(60), AirQualityLevel.GOOD, 2.0),
                pm10(KRAKOW, NOON.minus(Duration.ofDays(2)), AirQualityLevel.GOOD, 3.0)
            )
        );

        PageResult<AirQualityMeasurement> page = history.findByStation(KRAKOW.id(), THE_DAY, new PageRequest(0, 1));

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).singleElement()
            .extracting(AirQualityMeasurement::measuredAt)
            .isEqualTo(NOON.plusSeconds(60));
    }

    @Test
    void aMeasurementAtTheThresholdCountsAsOneSinceThen() {
        history.save(pm10(KRAKOW, NOON, AirQualityLevel.GOOD, 1.0));

        assertThat(history.hasMeasurementSince(KRAKOW.id(), NOON)).isTrue();
        assertThat(history.hasMeasurementSince(KRAKOW.id(), NOON.plusSeconds(1))).isFalse();
    }

    @Test
    void onlyWhatIsOlderThanTheThresholdIsDeleted() {
        history.saveAll(
            List.of(
                pm10(KRAKOW, NOON, AirQualityLevel.GOOD, 1.0),
                pm10(KRAKOW, NOON.plusSeconds(60), AirQualityLevel.GOOD, 2.0)
            )
        );

        long deleted = history.deleteOlderThan(NOON.plusSeconds(1));

        assertThat(deleted).isEqualTo(1);
        assertThat(history.findLatestByStationId(KRAKOW.id())).isPresent();
    }

    @Test
    void statisticsSummarizeTheMeasurementsOfTheWindow() {
        history.saveAll(
            List.of(
                measurement(KRAKOW, NOON, AirQualityLevel.GOOD, Map.of(Pollutant.PM10, 10.0, Pollutant.O3, 40.0)),
                measurement(KRAKOW, NOON.plusSeconds(60), AirQualityLevel.GOOD, Map.of(Pollutant.PM10, 20.0)),
                measurement(KRAKOW, NOON.plusSeconds(120), AirQualityLevel.BAD, Map.of(Pollutant.PM10, 30.0)),
                pm10(KRAKOW, NOON.minus(Duration.ofDays(2)), AirQualityLevel.BAD, 500.0),
                pm10(WARSAW, NOON, AirQualityLevel.BAD, 500.0)
            )
        );

        AirQualityStatistics statistics = analytics.statisticsFor(KRAKOW.id(), THE_DAY).orElseThrow();

        assertThat(statistics.stationName()).isEqualTo(KRAKOW.name());
        assertThat(statistics.measurementCount()).isEqualTo(3);
        assertThat(statistics.summaryFor(Pollutant.PM10))
            .contains(new AirQualityStatistics.PollutantSummary(20.0, 10.0, 30.0));
        assertThat(statistics.summaryFor(Pollutant.O3))
            .contains(new AirQualityStatistics.PollutantSummary(40.0, null, null));
        assertThat(statistics.summaryFor(Pollutant.SO2)).isEmpty();
        assertThat(statistics.levelDistribution()).containsEntry(AirQualityLevel.GOOD, 2)
            .containsEntry(AirQualityLevel.BAD, 1)
            .containsEntry(AirQualityLevel.MODERATE, 0);
    }

    @Test
    void aStationWithoutMeasurementsInTheWindowHasNoStatistics() {
        assertThat(analytics.statisticsFor(KRAKOW.id(), THE_DAY)).isEmpty();
    }

    @Test
    void theCleanestStationRanksFirst() {
        history.saveAll(List.of(unrated(KRAKOW, NOON, 80.0), unrated(WARSAW, NOON, 20.0)));

        List<StationRanking> ranking = analytics.ranking(THE_DAY, RankingLimit.of(10));

        assertThat(ranking).extracting(StationRanking::rank).containsExactly(1, 2);
        assertThat(ranking).extracting(StationRanking::station).containsExactly(WARSAW, KRAKOW);
        assertThat(ranking.getFirst().averageScore()).isEqualTo(20.0);
        assertThat(ranking.getFirst().measurementCount()).isEqualTo(1);
    }

    @Test
    void theDominantLevelIsTheMostFrequentAndATieGoesToTheWorse() {
        history.saveAll(
            List.of(
                pm10(KRAKOW, NOON, AirQualityLevel.GOOD, 10.0),
                pm10(KRAKOW, NOON.plusSeconds(60), AirQualityLevel.BAD, 10.0),
                pm10(WARSAW, NOON, AirQualityLevel.GOOD, 20.0),
                pm10(WARSAW, NOON.plusSeconds(60), AirQualityLevel.GOOD, 20.0),
                pm10(WARSAW, NOON.plusSeconds(120), AirQualityLevel.BAD, 20.0)
            )
        );

        List<StationRanking> ranking = analytics.ranking(THE_DAY, RankingLimit.of(10));

        assertThat(ranking).extracting(StationRanking::dominantQualityLevel)
            .containsExactly(AirQualityLevel.BAD, AirQualityLevel.GOOD);
    }

    @Test
    void aStationGoneFromTheCatalogueLeavesNoGapInTheRanks() {
        Station retired = station(99, "Retired");
        history.saveAll(List.of(unrated(retired, NOON, 1.0), unrated(WARSAW, NOON, 20.0)));

        List<StationRanking> ranking = analytics.ranking(THE_DAY, RankingLimit.of(10));

        assertThat(ranking).singleElement().extracting(StationRanking::rank).isEqualTo(1);
    }

    @Test
    void aWindowWithoutMeasurementsHasAnEmptyRanking() {
        assertThat(analytics.ranking(THE_DAY, RankingLimit.of(10))).isEmpty();
    }

    private static final class Catalogue implements StationCatalogue {
        @Override
        public Optional<Station> findById(int stationId) {
            return List.of(KRAKOW, WARSAW).stream().filter(station -> station.id() == stationId).findFirst();
        }

        @Override
        public List<Station> findAll() {
            return List.of(KRAKOW, WARSAW);
        }

        @Override
        public List<StationDistance> findWithin(Coordinates centre, SearchRadius radius) {
            return List.of();
        }
    }
}
