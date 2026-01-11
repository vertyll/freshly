package com.vertyll.freshly.airquality.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import com.vertyll.freshly.airquality.domain.*;

@ExtendWith(MockitoExtension.class)
class AirQualitySyncServiceTest {

    @Mock
    @SuppressWarnings("NullAway.Init")
    private AirQualityProvider airQualityProvider;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private AirQualityHistoryRepository historyRepository;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private ObjectProvider<AirQualitySyncService> selfProvider;

    @Captor
    @SuppressWarnings("NullAway.Init")
    private ArgumentCaptor<AirQualityMeasurement> measurementCaptor;

    private AirQualitySyncService syncService;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        syncService =
                new AirQualitySyncService(airQualityProvider, historyRepository, selfProvider);
    }

    @Test
    @DisplayName("Should trigger manual sync successfully")
    void shouldTriggerManualSyncSuccessfully() {
        // Given
        AirQualitySyncService spyService = spy(syncService);
        when(selfProvider.getObject()).thenReturn(spyService);
        doNothing().when(spyService).syncAirQualityData();

        // When
        syncService.triggerManualSync();

        // Then
        verify(selfProvider).getObject();
    }

    @Test
    @DisplayName("Should sync air quality data successfully")
    void shouldSyncAirQualityDataSuccessfully() {
        // Given
        Station station1 = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        Station station2 = new Station(124, "Station 2", "Krakow", "Street 2", 50.0647, 19.9450);
        List<Station> stations = List.of(station1, station2);

        AirQualityIndex index1 =
                new AirQualityIndex(
                        123,
                        LocalDateTime.now(ZoneOffset.UTC),
                        "Dobry",
                        "Dobry",
                        "Umiarkowany",
                        "Dobry");
        AirQualityIndex index2 =
                new AirQualityIndex(
                        124,
                        LocalDateTime.now(ZoneOffset.UTC),
                        "Umiarkowany",
                        "Dobry",
                        "Umiarkowany",
                        "Dobry");

        SensorMeasurement.Reading reading =
                new SensorMeasurement.Reading(LocalDateTime.now(ZoneOffset.UTC), 25.5);
        SensorMeasurement measurement =
                new SensorMeasurement(1, "PM10", "Pył zawieszony PM10", List.of(reading));

        when(airQualityProvider.findAllStations()).thenReturn(stations);
        when(historyRepository.hasRecentMeasurement(anyInt(), any(LocalDateTime.class)))
                .thenReturn(false);
        when(airQualityProvider.findIndexByStationId(123)).thenReturn(Optional.of(index1));
        when(airQualityProvider.findIndexByStationId(124)).thenReturn(Optional.of(index2));
        when(airQualityProvider.findMeasurementsByStationId(anyInt()))
                .thenReturn(List.of(measurement));
        when(historyRepository.save(any(AirQualityMeasurement.class)))
                .thenReturn(new AirQualityMeasurement());
        doNothing().when(historyRepository).deleteOlderThan(any(LocalDateTime.class));

        // When
        syncService.syncAirQualityData();

        // Then
        verify(airQualityProvider).findAllStations();
        verify(historyRepository, times(2)).save(any(AirQualityMeasurement.class));
        verify(historyRepository).deleteOlderThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should skip station with recent measurement")
    void shouldSkipStationWithRecentMeasurement() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        List<Station> stations = List.of(station);

        when(airQualityProvider.findAllStations()).thenReturn(stations);
        when(historyRepository.hasRecentMeasurement(eq(123), any(LocalDateTime.class)))
                .thenReturn(true);
        doNothing().when(historyRepository).deleteOlderThan(any(LocalDateTime.class));

        // When
        syncService.syncAirQualityData();

        // Then
        verify(airQualityProvider).findAllStations();
        verify(historyRepository).hasRecentMeasurement(eq(123), any(LocalDateTime.class));
        verify(airQualityProvider, never()).findIndexByStationId(anyInt());
        verify(historyRepository, never()).save(any(AirQualityMeasurement.class));
    }

    @Test
    @DisplayName("Should skip station when index data not available")
    void shouldSkipStationWhenIndexNotAvailable() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        List<Station> stations = List.of(station);

        when(airQualityProvider.findAllStations()).thenReturn(stations);
        when(historyRepository.hasRecentMeasurement(anyInt(), any(LocalDateTime.class)))
                .thenReturn(false);
        when(airQualityProvider.findIndexByStationId(123)).thenReturn(Optional.empty());
        doNothing().when(historyRepository).deleteOlderThan(any(LocalDateTime.class));

        // When
        syncService.syncAirQualityData();

        // Then
        verify(airQualityProvider).findAllStations();
        verify(airQualityProvider).findIndexByStationId(123);
        verify(historyRepository, never()).save(any(AirQualityMeasurement.class));
    }

    @Test
    @DisplayName("Should handle exception during station sync gracefully")
    void shouldHandleExceptionDuringStationSyncGracefully() {
        // Given
        Station station1 = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        Station station2 = new Station(124, "Station 2", "Krakow", "Street 2", 50.0647, 19.9450);
        List<Station> stations = List.of(station1, station2);

        AirQualityIndex index2 =
                new AirQualityIndex(
                        124, LocalDateTime.now(ZoneOffset.UTC), "Dobry", "Dobry", "Dobry", "Dobry");

        SensorMeasurement.Reading reading =
                new SensorMeasurement.Reading(LocalDateTime.now(ZoneOffset.UTC), 25.5);
        SensorMeasurement measurement =
                new SensorMeasurement(1, "PM10", "Pył zawieszony PM10", List.of(reading));

        when(airQualityProvider.findAllStations()).thenReturn(stations);
        when(historyRepository.hasRecentMeasurement(anyInt(), any(LocalDateTime.class)))
                .thenReturn(false);
        when(airQualityProvider.findIndexByStationId(123))
                .thenThrow(new RuntimeException("API Error"));
        when(airQualityProvider.findIndexByStationId(124)).thenReturn(Optional.of(index2));
        when(airQualityProvider.findMeasurementsByStationId(124)).thenReturn(List.of(measurement));
        when(historyRepository.save(any(AirQualityMeasurement.class)))
                .thenReturn(new AirQualityMeasurement());
        doNothing().when(historyRepository).deleteOlderThan(any(LocalDateTime.class));

        // When
        syncService.syncAirQualityData();

        // Then
        verify(airQualityProvider).findAllStations();
        verify(airQualityProvider).findIndexByStationId(123);
        verify(airQualityProvider).findIndexByStationId(124);
        verify(historyRepository, times(1))
                .save(any(AirQualityMeasurement.class)); // Only station2 saved
    }

    @Test
    @DisplayName("Should save measurement with correct sensor values")
    void shouldSaveMeasurementWithCorrectSensorValues() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        List<Station> stations = List.of(station);

        AirQualityIndex index =
                new AirQualityIndex(
                        123,
                        LocalDateTime.now(ZoneOffset.UTC),
                        "Dobry",
                        "Dobry",
                        "Umiarkowany",
                        "Dobry");

        SensorMeasurement.Reading pm10Reading =
                new SensorMeasurement.Reading(LocalDateTime.now(ZoneOffset.UTC), 25.5);
        SensorMeasurement.Reading pm25Reading =
                new SensorMeasurement.Reading(LocalDateTime.now(ZoneOffset.UTC), 15.2);
        SensorMeasurement pm10Measurement =
                new SensorMeasurement(1, "PM10", "PM10", List.of(pm10Reading));
        SensorMeasurement pm25Measurement =
                new SensorMeasurement(2, "PM2.5", "PM2.5", List.of(pm25Reading));

        when(airQualityProvider.findAllStations()).thenReturn(stations);
        when(historyRepository.hasRecentMeasurement(anyInt(), any(LocalDateTime.class)))
                .thenReturn(false);
        when(airQualityProvider.findIndexByStationId(123)).thenReturn(Optional.of(index));
        when(airQualityProvider.findMeasurementsByStationId(123))
                .thenReturn(List.of(pm10Measurement, pm25Measurement));
        when(historyRepository.save(any(AirQualityMeasurement.class)))
                .thenReturn(new AirQualityMeasurement());
        doNothing().when(historyRepository).deleteOlderThan(any(LocalDateTime.class));

        // When
        syncService.syncAirQualityData();

        // Then
        verify(historyRepository).save(measurementCaptor.capture());
        AirQualityMeasurement savedMeasurement = measurementCaptor.getValue();

        assertThat(savedMeasurement.getStationId()).isEqualTo(123);
        assertThat(savedMeasurement.getStationName()).isEqualTo("Station 1");
        assertThat(savedMeasurement.getPm10Value()).isEqualTo(25.5);
        assertThat(savedMeasurement.getPm25Value()).isEqualTo(15.2);
    }

    @Test
    @DisplayName("Should cleanup old data after sync")
    void shouldCleanupOldDataAfterSync() {
        // Given
        when(airQualityProvider.findAllStations()).thenReturn(Collections.emptyList());
        doNothing().when(historyRepository).deleteOlderThan(any(LocalDateTime.class));

        // When
        syncService.syncAirQualityData();

        // Then
        verify(historyRepository).deleteOlderThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should handle cleanup exception gracefully")
    void shouldHandleCleanupExceptionGracefully() {
        // Given
        when(airQualityProvider.findAllStations()).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("Cleanup error"))
                .when(historyRepository)
                .deleteOlderThan(any(LocalDateTime.class));

        // When & Then
        assertThatCode(syncService::syncAirQualityData).doesNotThrowAnyException();

        verify(historyRepository).deleteOlderThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should extract latest sensor values correctly")
    void shouldExtractLatestSensorValuesCorrectly() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        List<Station> stations = List.of(station);

        AirQualityIndex index =
                new AirQualityIndex(
                        123, LocalDateTime.now(ZoneOffset.UTC), "Dobry", "Dobry", "Dobry", "Dobry");

        // Multiple readings with different timestamps - should pick the first non-null
        SensorMeasurement.Reading reading1 =
                new SensorMeasurement.Reading(LocalDateTime.now(ZoneOffset.UTC), 25.5);
        SensorMeasurement.Reading reading2 =
                new SensorMeasurement.Reading(
                        LocalDateTime.now(ZoneOffset.UTC).minusHours(1), 30.2);
        SensorMeasurement measurement =
                new SensorMeasurement(1, "PM10", "PM10", List.of(reading1, reading2));

        when(airQualityProvider.findAllStations()).thenReturn(stations);
        when(historyRepository.hasRecentMeasurement(anyInt(), any(LocalDateTime.class)))
                .thenReturn(false);
        when(airQualityProvider.findIndexByStationId(123)).thenReturn(Optional.of(index));
        when(airQualityProvider.findMeasurementsByStationId(123)).thenReturn(List.of(measurement));
        when(historyRepository.save(any(AirQualityMeasurement.class)))
                .thenReturn(new AirQualityMeasurement());
        doNothing().when(historyRepository).deleteOlderThan(any(LocalDateTime.class));

        // When
        syncService.syncAirQualityData();

        // Then
        verify(historyRepository).save(measurementCaptor.capture());
        AirQualityMeasurement savedMeasurement = measurementCaptor.getValue();

        assertThat(savedMeasurement.getPm10Value()).isEqualTo(25.5); // Should be the first reading
    }

    @Test
    @DisplayName("Should handle empty sensor readings")
    void shouldHandleEmptySensorReadings() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        List<Station> stations = List.of(station);

        AirQualityIndex index =
                new AirQualityIndex(
                        123, LocalDateTime.now(ZoneOffset.UTC), "Dobry", "Dobry", "Dobry", "Dobry");

        SensorMeasurement emptyMeasurement =
                new SensorMeasurement(1, "PM10", "PM10", Collections.emptyList());

        when(airQualityProvider.findAllStations()).thenReturn(stations);
        when(historyRepository.hasRecentMeasurement(anyInt(), any(LocalDateTime.class)))
                .thenReturn(false);
        when(airQualityProvider.findIndexByStationId(123)).thenReturn(Optional.of(index));
        when(airQualityProvider.findMeasurementsByStationId(123))
                .thenReturn(List.of(emptyMeasurement));
        when(historyRepository.save(any(AirQualityMeasurement.class)))
                .thenReturn(new AirQualityMeasurement());
        doNothing().when(historyRepository).deleteOlderThan(any(LocalDateTime.class));

        // When
        syncService.syncAirQualityData();

        // Then
        verify(historyRepository).save(measurementCaptor.capture());
        AirQualityMeasurement savedMeasurement = measurementCaptor.getValue();

        assertThat(savedMeasurement.getPm10Value()).isNull(); // No reading available
    }

    @Test
    @DisplayName("Should skip null sensor readings")
    @SuppressWarnings("NullAway")
    void shouldSkipNullSensorReadings() {
        // Given
        Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        List<Station> stations = List.of(station);

        AirQualityIndex index =
                new AirQualityIndex(
                        123, LocalDateTime.now(ZoneOffset.UTC), "Dobry", "Dobry", "Dobry", "Dobry");

        SensorMeasurement.Reading nullReading =
                new SensorMeasurement.Reading(LocalDateTime.now(ZoneOffset.UTC), null);
        SensorMeasurement.Reading validReading =
                new SensorMeasurement.Reading(LocalDateTime.now(ZoneOffset.UTC), 25.5);
        SensorMeasurement measurement =
                new SensorMeasurement(1, "PM10", "PM10", List.of(nullReading, validReading));

        when(airQualityProvider.findAllStations()).thenReturn(stations);
        when(historyRepository.hasRecentMeasurement(anyInt(), any(LocalDateTime.class)))
                .thenReturn(false);
        when(airQualityProvider.findIndexByStationId(123)).thenReturn(Optional.of(index));
        when(airQualityProvider.findMeasurementsByStationId(123)).thenReturn(List.of(measurement));
        when(historyRepository.save(any(AirQualityMeasurement.class)))
                .thenReturn(new AirQualityMeasurement());
        doNothing().when(historyRepository).deleteOlderThan(any(LocalDateTime.class));

        // When
        syncService.syncAirQualityData();

        // Then
        verify(historyRepository).save(measurementCaptor.capture());
        AirQualityMeasurement savedMeasurement = measurementCaptor.getValue();

        assertThat(savedMeasurement.getPm10Value())
                .isEqualTo(25.5); // Should skip null and use valid
    }

    @Test
    @DisplayName("Should continue sync even when some stations fail")
    void shouldContinueSyncWhenSomeStationsFail() {
        // Given
        Station station1 = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
        Station station2 = new Station(124, "Station 2", "Krakow", "Street 2", 50.0647, 19.9450);
        Station station3 = new Station(125, "Station 3", "Gdansk", "Street 3", 54.3520, 18.6466);
        List<Station> stations = List.of(station1, station2, station3);

        AirQualityIndex index3 =
                new AirQualityIndex(
                        125, LocalDateTime.now(ZoneOffset.UTC), "Dobry", "Dobry", "Dobry", "Dobry");

        SensorMeasurement.Reading reading =
                new SensorMeasurement.Reading(LocalDateTime.now(ZoneOffset.UTC), 25.5);
        SensorMeasurement measurement = new SensorMeasurement(1, "PM10", "PM10", List.of(reading));

        when(airQualityProvider.findAllStations()).thenReturn(stations);
        when(historyRepository.hasRecentMeasurement(anyInt(), any(LocalDateTime.class)))
                .thenReturn(false);
        when(airQualityProvider.findIndexByStationId(123))
                .thenThrow(new RuntimeException("API Error 1"));
        when(airQualityProvider.findIndexByStationId(124)).thenReturn(Optional.empty());
        when(airQualityProvider.findIndexByStationId(125)).thenReturn(Optional.of(index3));
        when(airQualityProvider.findMeasurementsByStationId(125)).thenReturn(List.of(measurement));
        when(historyRepository.save(any(AirQualityMeasurement.class)))
                .thenReturn(new AirQualityMeasurement());
        doNothing().when(historyRepository).deleteOlderThan(any(LocalDateTime.class));

        // When
        syncService.syncAirQualityData();

        // Then
        verify(airQualityProvider).findAllStations();
        verify(historyRepository, times(1))
                .save(any(AirQualityMeasurement.class)); // Only station3 saved
        verify(historyRepository).deleteOlderThan(any(LocalDateTime.class));
    }
}
