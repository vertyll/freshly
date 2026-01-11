package com.vertyll.freshly.airquality.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vertyll.freshly.airquality.domain.*;

@ExtendWith(MockitoExtension.class)
class AirQualityServiceTest {

    @Mock
    @SuppressWarnings("NullAway.Init")
    private AirQualityProvider airQualityProvider;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private AirQualityHistoryRepository historyRepository;

    @InjectMocks
    @SuppressWarnings("NullAway.Init")
    private AirQualityService airQualityService;

    @Nested
    @DisplayName("Get All Stations Tests")
    class GetAllStationsTests {

        @Test
        @DisplayName("Should get all stations successfully")
        void shouldGetAllStationsSuccessfully() {
            // Given
            Station station1 =
                    new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
            Station station2 =
                    new Station(124, "Station 2", "Krakow", "Street 2", 50.0647, 19.9450);
            List<Station> stations = List.of(station1, station2);

            when(airQualityProvider.findAllStations()).thenReturn(stations);

            // When
            List<Station> result = airQualityService.getAllStations();

            // Then
            assertThat(result).hasSize(2).containsExactly(station1, station2);
            verify(airQualityProvider).findAllStations();
        }

        @Test
        @DisplayName("Should return empty list when no stations available")
        void shouldReturnEmptyListWhenNoStations() {
            // Given
            when(airQualityProvider.findAllStations()).thenReturn(Collections.emptyList());

            // When
            List<Station> result = airQualityService.getAllStations();

            // Then
            assertThat(result).isEmpty();
            verify(airQualityProvider).findAllStations();
        }
    }

    @Nested
    @DisplayName("Get Index For Station Tests")
    class GetIndexForStationTests {

        @Test
        @DisplayName("Should get index for station successfully")
        void shouldGetIndexForStationSuccessfully() {
            // Given
            int stationId = 123;
            AirQualityIndex index =
                    new AirQualityIndex(
                            stationId,
                            LocalDateTime.now(ZoneOffset.UTC),
                            "Dobry",
                            "Dobry",
                            "Umiarkowany",
                            "Dobry");

            when(airQualityProvider.findIndexByStationId(stationId)).thenReturn(Optional.of(index));

            // When
            Optional<AirQualityIndex> result = airQualityService.getIndexForStation(stationId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().stationId()).isEqualTo(stationId);
            assertThat(result.get().stIndexLevel()).isEqualTo("Dobry");
            verify(airQualityProvider).findIndexByStationId(stationId);
        }

        @Test
        @DisplayName("Should return empty when index not found")
        void shouldReturnEmptyWhenIndexNotFound() {
            // Given
            int stationId = 999;
            when(airQualityProvider.findIndexByStationId(stationId)).thenReturn(Optional.empty());

            // When
            Optional<AirQualityIndex> result = airQualityService.getIndexForStation(stationId);

            // Then
            assertThat(result).isEmpty();
            verify(airQualityProvider).findIndexByStationId(stationId);
        }
    }

    @Nested
    @DisplayName("Get Measurements For Station Tests")
    class GetMeasurementsForStationTests {

        @Test
        @DisplayName("Should get measurements for station successfully")
        void shouldGetMeasurementsForStationSuccessfully() {
            // Given
            int stationId = 123;
            SensorMeasurement.Reading reading1 =
                    new SensorMeasurement.Reading(LocalDateTime.now(ZoneOffset.UTC), 25.5);
            SensorMeasurement.Reading reading2 =
                    new SensorMeasurement.Reading(
                            LocalDateTime.now(ZoneOffset.UTC).minusHours(1), 30.2);
            SensorMeasurement measurement =
                    new SensorMeasurement(
                            1, "PM10", "Pył zawieszony PM10", List.of(reading1, reading2));
            List<SensorMeasurement> measurements = List.of(measurement);

            when(airQualityProvider.findMeasurementsByStationId(stationId))
                    .thenReturn(measurements);

            // When
            List<SensorMeasurement> result = airQualityService.getMeasurementsForStation(stationId);

            // Then
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(
                            m -> {
                                assertThat(m.paramCode()).isEqualTo("PM10");
                                assertThat(m.readings()).hasSize(2);
                            });
            verify(airQualityProvider).findMeasurementsByStationId(stationId);
        }

        @Test
        @DisplayName("Should return empty list when no measurements")
        void shouldReturnEmptyListWhenNoMeasurements() {
            // Given
            int stationId = 123;
            when(airQualityProvider.findMeasurementsByStationId(stationId))
                    .thenReturn(Collections.emptyList());

            // When
            List<SensorMeasurement> result = airQualityService.getMeasurementsForStation(stationId);

            // Then
            assertThat(result).isEmpty();
            verify(airQualityProvider).findMeasurementsByStationId(stationId);
        }
    }

    @Nested
    @DisplayName("Get Latest Measurement Tests")
    class GetLatestMeasurementTests {

        @Test
        @DisplayName("Should get latest measurement successfully")
        void shouldGetLatestMeasurementSuccessfully() {
            // Given
            int stationId = 123;
            AirQualityMeasurement measurement = new AirQualityMeasurement();
            measurement.setStationId(stationId);
            measurement.setStationName("Station 1");
            measurement.setOverallIndexLevel(AirQualityLevel.GOOD);
            measurement.setPm10Value(25.5);

            when(historyRepository.findLatestByStationId(stationId))
                    .thenReturn(Optional.of(measurement));

            // When
            Optional<AirQualityMeasurement> result =
                    airQualityService.getLatestMeasurement(stationId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getStationId()).isEqualTo(stationId);
            assertThat(result.get().getPm10Value()).isEqualTo(25.5);
            verify(historyRepository).findLatestByStationId(stationId);
        }

        @Test
        @DisplayName("Should return empty when latest measurement not found")
        void shouldReturnEmptyWhenLatestMeasurementNotFound() {
            // Given
            int stationId = 999;
            when(historyRepository.findLatestByStationId(stationId)).thenReturn(Optional.empty());

            // When
            Optional<AirQualityMeasurement> result =
                    airQualityService.getLatestMeasurement(stationId);

            // Then
            assertThat(result).isEmpty();
            verify(historyRepository).findLatestByStationId(stationId);
        }
    }

    @Nested
    @DisplayName("Get Historical Measurements Tests")
    class GetHistoricalMeasurementsTests {

        @Test
        @DisplayName("Should get historical measurements with valid days")
        void shouldGetHistoricalMeasurementsWithValidDays() {
            // Given
            int stationId = 123;
            int days = 7;

            List<AirQualityMeasurement> measurements = List.of(new AirQualityMeasurement());
            when(historyRepository.findByStationIdAndDateRange(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(measurements);

            // When
            List<AirQualityMeasurement> result =
                    airQualityService.getHistoricalMeasurements(stationId, days);

            // Then
            assertThat(result).hasSize(1);
            verify(historyRepository)
                    .findByStationIdAndDateRange(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should clamp days to minimum of 1")
        void shouldClampDaysToMinimum() {
            // Given
            int stationId = 123;
            int invalidDays = 0;
            when(historyRepository.findByStationIdAndDateRange(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<AirQualityMeasurement> result =
                    airQualityService.getHistoricalMeasurements(stationId, invalidDays);

            // Then
            assertThat(result).isEmpty();
            verify(historyRepository)
                    .findByStationIdAndDateRange(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should clamp days to maximum of 90")
        void shouldClampDaysToMaximum() {
            // Given
            int stationId = 123;
            int invalidDays = 100;
            when(historyRepository.findByStationIdAndDateRange(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<AirQualityMeasurement> result =
                    airQualityService.getHistoricalMeasurements(stationId, invalidDays);

            // Then
            assertThat(result).isEmpty();
            verify(historyRepository)
                    .findByStationIdAndDateRange(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Get Measurements In Range Tests")
    class GetMeasurementsInRangeTests {

        @Test
        @DisplayName("Should get measurements in range successfully")
        void shouldGetMeasurementsInRangeSuccessfully() {
            // Given
            LocalDateTime from = LocalDateTime.now(ZoneOffset.UTC).minusDays(7);
            LocalDateTime to = LocalDateTime.now(ZoneOffset.UTC);
            List<AirQualityMeasurement> measurements = List.of(new AirQualityMeasurement());

            when(historyRepository.findByDateRange(from, to)).thenReturn(measurements);

            // When
            List<AirQualityMeasurement> result = airQualityService.getMeasurementsInRange(from, to);

            // Then
            assertThat(result).hasSize(1);
            verify(historyRepository).findByDateRange(from, to);
        }
    }

    @Nested
    @DisplayName("Find Nearest Stations Tests")
    class FindNearestStationsTests {

        @Test
        @DisplayName("Should find nearest stations within radius")
        void shouldFindNearestStationsWithinRadius() {
            // Given
            double latitude = 52.2297;
            double longitude = 21.0122;
            double radius = 10;

            Station station1 =
                    new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
            Station station2 =
                    new Station(124, "Station 2", "Warsaw", "Street 2", 52.2397, 21.0222);
            Station station3 =
                    new Station(125, "Station 3", "Krakow", "Street 3", 50.0647, 19.9450);
            List<Station> allStations = List.of(station1, station2, station3);

            when(airQualityProvider.findAllStations()).thenReturn(allStations);

            // When
            List<StationDistance> result =
                    airQualityService.findNearestStations(latitude, longitude, radius);

            // Then
            assertThat(result)
                    .isNotEmpty()
                    .allMatch(sd -> sd.distanceInKm() <= radius)
                    .isSortedAccordingTo(Comparator.comparingDouble(StationDistance::distanceInKm));
            verify(airQualityProvider).findAllStations();
        }

        @Test
        @DisplayName("Should return empty list when no stations within radius")
        void shouldReturnEmptyListWhenNoStationsWithinRadius() {
            // Given
            double latitude = 52.2297;
            double longitude = 21.0122;
            double radius = 0.1; // Very small radius

            Station station =
                    new Station(125, "Station Far", "Krakow", "Street 3", 50.0647, 19.9450);
            List<Station> allStations = List.of(station);

            when(airQualityProvider.findAllStations()).thenReturn(allStations);

            // When
            List<StationDistance> result =
                    airQualityService.findNearestStations(latitude, longitude, radius);

            // Then
            assertThat(result).isEmpty();
            verify(airQualityProvider).findAllStations();
        }

        @Test
        @DisplayName("Should clamp radius to minimum of 1")
        void shouldClampRadiusToMinimum() {
            // Given
            double latitude = 52.2297;
            double longitude = 21.0122;
            double invalidRadius = 0.5;

            when(airQualityProvider.findAllStations()).thenReturn(Collections.emptyList());

            // When
            List<StationDistance> result =
                    airQualityService.findNearestStations(latitude, longitude, invalidRadius);

            // Then
            assertThat(result).isEmpty();
            verify(airQualityProvider).findAllStations();
        }

        @Test
        @DisplayName("Should clamp radius to maximum of 100")
        void shouldClampRadiusToMaximum() {
            // Given
            double latitude = 52.2297;
            double longitude = 21.0122;
            double invalidRadius = 150;

            when(airQualityProvider.findAllStations()).thenReturn(Collections.emptyList());

            // When
            List<StationDistance> result =
                    airQualityService.findNearestStations(latitude, longitude, invalidRadius);

            // Then
            assertThat(result).isEmpty();
            verify(airQualityProvider).findAllStations();
        }

        @Test
        @DisplayName("Should sort stations by distance")
        void shouldSortStationsByDistance() {
            // Given
            double latitude = 52.2297;
            double longitude = 21.0122;
            double radius = 50;

            Station nearStation =
                    new Station(123, "Near Station", "Warsaw", "Street 1", 52.2297, 21.0122);
            Station midStation =
                    new Station(124, "Mid Station", "Warsaw", "Street 2", 52.2497, 21.0322);
            Station farStation =
                    new Station(125, "Far Station", "Warsaw", "Street 3", 52.2697, 21.0522);
            List<Station> allStations = List.of(farStation, nearStation, midStation); // Not sorted

            when(airQualityProvider.findAllStations()).thenReturn(allStations);

            // When
            List<StationDistance> result =
                    airQualityService.findNearestStations(latitude, longitude, radius);

            // Then
            assertThat(result)
                    .isSortedAccordingTo(Comparator.comparingDouble(StationDistance::distanceInKm));

            final int minRequiredSizeForComparison = 2;
            if (result.size() >= minRequiredSizeForComparison) {
                assertThat(result.get(0).distanceInKm())
                        .isLessThanOrEqualTo(result.get(1).distanceInKm());
            }

            verify(airQualityProvider).findAllStations();
        }
    }

    @Nested
    @DisplayName("Get Statistics Tests")
    class GetStatisticsTests {

        @Test
        @DisplayName("Should get statistics successfully")
        void shouldGetStatisticsSuccessfully() {
            // Given
            int stationId = 123;
            int days = 7;
            AirQualityStatistics statistics =
                    new AirQualityStatistics(
                            stationId,
                            "Station 1",
                            LocalDateTime.now(ZoneOffset.UTC).minusDays(days),
                            LocalDateTime.now(ZoneOffset.UTC),
                            100,
                            25.5,
                            10.0,
                            50.0,
                            15.2,
                            5.0,
                            30.0,
                            8.5,
                            12.3,
                            0.5,
                            45.2,
                            20,
                            30,
                            25,
                            15,
                            8,
                            2);

            when(historyRepository.calculateStatistics(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(statistics));

            // When
            Optional<AirQualityStatistics> result =
                    airQualityService.getStatistics(stationId, days);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().stationId()).isEqualTo(stationId);
            assertThat(result.get().measurementCount()).isEqualTo(100);
            verify(historyRepository)
                    .calculateStatistics(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should return empty when no statistics available")
        void shouldReturnEmptyWhenNoStatistics() {
            // Given
            int stationId = 999;
            int days = 7;

            when(historyRepository.calculateStatistics(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // When
            Optional<AirQualityStatistics> result =
                    airQualityService.getStatistics(stationId, days);

            // Then
            assertThat(result).isEmpty();
            verify(historyRepository)
                    .calculateStatistics(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should clamp days parameter")
        void shouldClampDaysParameter() {
            // Given
            int stationId = 123;
            int invalidDays = 100;

            when(historyRepository.calculateStatistics(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // When
            Optional<AirQualityStatistics> result =
                    airQualityService.getStatistics(stationId, invalidDays);

            // Then
            assertThat(result).isEmpty();
            verify(historyRepository)
                    .calculateStatistics(
                            eq(stationId), any(LocalDateTime.class), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Get Ranking Tests")
    class GetRankingTests {

        @Test
        @DisplayName("Should get ranking successfully")
        void shouldGetRankingSuccessfully() {
            // Given
            int days = 7;
            int limit = 10;
            Station station1 =
                    new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
            Station station2 =
                    new Station(124, "Station 2", "Krakow", "Street 2", 50.0647, 19.9450);
            StationRanking ranking1 =
                    new StationRanking(1, station1, 25.5, AirQualityLevel.GOOD, 90);
            StationRanking ranking2 =
                    new StationRanking(2, station2, 35.5, AirQualityLevel.MODERATE, 85);
            List<StationRanking> rankings = List.of(ranking1, ranking2);

            when(historyRepository.getRanking(
                            any(LocalDateTime.class), any(LocalDateTime.class), eq(limit)))
                    .thenReturn(rankings);

            // When
            List<StationRanking> result = airQualityService.getRanking(days, limit);

            // Then
            assertThat(result).hasSize(2).first().extracting(r -> r.station().id()).isEqualTo(123);
            verify(historyRepository)
                    .getRanking(any(LocalDateTime.class), any(LocalDateTime.class), eq(limit));
        }

        @Test
        @DisplayName("Should clamp days to valid range")
        void shouldClampDaysToValidRange() {
            // Given
            int invalidDays = 100;
            int limit = 10;

            when(historyRepository.getRanking(
                            any(LocalDateTime.class), any(LocalDateTime.class), eq(limit)))
                    .thenReturn(Collections.emptyList());

            // When
            List<StationRanking> result = airQualityService.getRanking(invalidDays, limit);

            // Then
            assertThat(result).isEmpty();
            verify(historyRepository)
                    .getRanking(any(LocalDateTime.class), any(LocalDateTime.class), eq(limit));
        }

        @Test
        @DisplayName("Should clamp limit to valid range")
        void shouldClampLimitToValidRange() {
            // Given
            int days = 7;
            int invalidLimit = 100;

            when(historyRepository.getRanking(
                            any(LocalDateTime.class), any(LocalDateTime.class), eq(50)))
                    .thenReturn(Collections.emptyList());

            // When
            List<StationRanking> result = airQualityService.getRanking(days, invalidLimit);

            // Then
            assertThat(result).isEmpty();
            verify(historyRepository)
                    .getRanking(any(LocalDateTime.class), any(LocalDateTime.class), eq(50));
        }

        @Test
        @DisplayName("Should return empty list when no rankings")
        void shouldReturnEmptyListWhenNoRankings() {
            // Given
            int days = 7;
            int limit = 10;

            when(historyRepository.getRanking(
                            any(LocalDateTime.class), any(LocalDateTime.class), eq(limit)))
                    .thenReturn(Collections.emptyList());

            // When
            List<StationRanking> result = airQualityService.getRanking(days, limit);

            // Then
            assertThat(result).isEmpty();
            verify(historyRepository)
                    .getRanking(any(LocalDateTime.class), any(LocalDateTime.class), eq(limit));
        }
    }
}
