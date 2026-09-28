package com.vertyll.freshly.airquality.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.airquality.application.dto.SyncReport;
import com.vertyll.freshly.airquality.application.port.outbound.AirQualityProviderPort;
import com.vertyll.freshly.airquality.application.service.command.AirQualitySyncService;
import com.vertyll.freshly.airquality.domain.model.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.model.AirQualityLevel;
import com.vertyll.freshly.airquality.domain.model.AirQualityMeasurement;
import com.vertyll.freshly.airquality.domain.model.AnalysisWindow;
import com.vertyll.freshly.airquality.domain.model.Coordinates;
import com.vertyll.freshly.airquality.domain.model.Pollutant;
import com.vertyll.freshly.airquality.domain.model.SearchRadius;
import com.vertyll.freshly.airquality.domain.model.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.model.Station;
import com.vertyll.freshly.airquality.domain.model.StationDistance;
import com.vertyll.freshly.airquality.domain.repository.AirQualityHistoryRepository;
import com.vertyll.freshly.airquality.domain.repository.StationCatalogue;
import com.vertyll.freshly.lang.logging.RecordingUseCaseLogger;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PageResult;

import static org.assertj.core.api.Assertions.assertThat;

class AirQualitySyncServiceTest {
    private static final Station WARSAW =
            new Station(1, "Warszawa", "Warszawa", "Marszałkowska", new Coordinates(52.2, 21.0));
    private static final Station KRAKOW = new Station(2, "Kraków", "Kraków", "Floriańska", new Coordinates(50.0, 19.9));

    private FakeProvider provider;
    private InMemoryHistory history;
    private AirQualitySyncService service;

    @BeforeEach
    void setUp() {
        provider = new FakeProvider();
        history = new InMemoryHistory();
        service = new AirQualitySyncService(provider, new ProviderCatalogue(provider), history, new RecordingUseCaseLogger());
    }

    @Test
    @DisplayName("one station failing does not abandon the run")
    void isolatesPerStationFailure() {
        provider.stations(WARSAW, KRAKOW);
        provider.indexFor(KRAKOW.id(), AirQualityLevel.GOOD);
        provider.failIndexFor(WARSAW.id());

        SyncReport report = service.syncAll();

        assertThat(report.failed()).isEqualTo(1);
        assertThat(report.synced()).isEqualTo(1);
        assertThat(history.saved).hasSize(1);
    }

    @Test
    @DisplayName("a run where nothing stored is distinguishable from a quiet success")
    void reportsTotalFailure() {
        provider.stations(WARSAW);
        provider.failIndexFor(WARSAW.id());

        assertThat(service.syncAll().completelyFailed()).isTrue();
    }

    @Test
    @DisplayName("a station with no published index is skipped, not failed")
    void missingIndexIsSkipped() {
        provider.stations(WARSAW);

        SyncReport report = service.syncAll();

        assertThat(report.skipped()).isEqualTo(1);
        assertThat(report.failed()).isZero();
    }

    @Test
    @DisplayName("a station whose data is still fresh is not re-fetched")
    void skipsFreshStations() {
        provider.stations(WARSAW);
        provider.indexFor(WARSAW.id(), AirQualityLevel.GOOD);
        history.fresh(WARSAW.id());

        SyncReport report = service.syncAll();

        assertThat(report.skipped()).isEqualTo(1);
        assertThat(history.saved).isEmpty();
    }

    @Test
    @DisplayName("null sensor readings are dropped, never stored as zero")
    void dropsNullReadings() {
        provider.stations(WARSAW);
        provider.indexFor(WARSAW.id(), AirQualityLevel.MODERATE);
        provider.sensor(WARSAW.id(), Pollutant.PM10, null);
        provider.sensor(WARSAW.id(), Pollutant.PM25, 21.0);

        service.syncAll();

        AirQualityMeasurement stored = history.saved.getFirst();
        assertThat(stored.readingFor(Pollutant.PM10)).isEmpty();
        assertThat(stored.readingFor(Pollutant.PM25)).contains(21.0);
    }

    @Test
    @DisplayName("the latest reading wins when a sensor reported more than once")
    void keepsLatestReading() {
        provider.stations(WARSAW);
        provider.indexFor(WARSAW.id(), AirQualityLevel.GOOD);
        provider.sensorSeries(
            WARSAW.id(),
            Pollutant.PM10,
            new SensorMeasurement.Reading(Instant.now().minusSeconds(3600), 40.0),
            new SensorMeasurement.Reading(Instant.now(), 12.0)
        );

        service.syncAll();

        assertThat(history.saved.getFirst().readingFor(Pollutant.PM10)).contains(12.0);
    }

    private static final class FakeProvider implements AirQualityProviderPort {
        private final List<Station> stations = new ArrayList<>();
        private final Map<Integer, AirQualityLevel> indexes = new HashMap<>();
        private final Set<Integer> failing = new HashSet<>();
        private final Map<Integer, List<SensorMeasurement>> sensors = new HashMap<>();

        void stations(Station... all) {
            stations.addAll(List.of(all));
        }

        void indexFor(int stationId, AirQualityLevel level) {
            indexes.put(stationId, level);
        }

        void failIndexFor(int stationId) {
            failing.add(stationId);
        }

        void sensor(int stationId, Pollutant pollutant, Double value) {
            sensorSeries(stationId, pollutant, new SensorMeasurement.Reading(Instant.now(), value));
        }

        void sensorSeries(int stationId, Pollutant pollutant, SensorMeasurement.Reading... readings) {
            sensors.computeIfAbsent(stationId, id -> new ArrayList<>())
                .add(new SensorMeasurement(stationId * 10, pollutant, List.of(readings)));
        }

        @Override
        public List<Station> findAllStations() {
            return stations;
        }

        @Override
        public Optional<AirQualityIndex> findCurrentIndex(int stationId) {
            if (failing.contains(stationId)) {
                throw new IllegalStateException("GIOŚ unavailable");
            }
            AirQualityLevel level = indexes.get(stationId);
            return level == null ? Optional.empty()
                    : Optional.of(new AirQualityIndex(stationId, Instant.now(), level, null, null, level, null, null));
        }

        @Override
        public List<SensorMeasurement> findSensorMeasurements(int stationId) {
            return sensors.getOrDefault(stationId, List.of());
        }
    }

    private record ProviderCatalogue(FakeProvider provider) implements StationCatalogue {
        @Override
        public Optional<Station> findById(int stationId) {
            return provider.findAllStations().stream().filter(station -> station.id() == stationId).findFirst();
        }

        @Override
        public List<Station> findAll() {
            return provider.findAllStations();
        }

        @Override
        public List<StationDistance> findWithin(Coordinates centre, SearchRadius radius) {
            return List.of();
        }
    }

    private static final class InMemoryHistory implements AirQualityHistoryRepository {
        private final List<AirQualityMeasurement> saved = new ArrayList<>();
        private final Set<Integer> freshStations = new HashSet<>();

        void fresh(int stationId) {
            freshStations.add(stationId);
        }

        @Override
        public AirQualityMeasurement save(AirQualityMeasurement measurement) {
            saved.add(measurement);
            return measurement;
        }

        @Override
        public List<AirQualityMeasurement> saveAll(List<AirQualityMeasurement> measurements) {
            saved.addAll(measurements);
            return measurements;
        }

        @Override
        public Optional<AirQualityMeasurement> findLatestByStationId(int stationId) {
            return saved.stream().filter(m -> m.stationId() == stationId).findFirst();
        }

        @Override
        public PageResult<AirQualityMeasurement> findByStation(int stationId, AnalysisWindow window, PageRequest page) {
            return PageResult.empty(page);
        }

        @Override
        public boolean hasMeasurementSince(int stationId, Instant threshold) {
            return freshStations.contains(stationId);
        }

        @Override
        public long deleteOlderThan(Instant threshold) {
            return 0L;
        }
    }
}
