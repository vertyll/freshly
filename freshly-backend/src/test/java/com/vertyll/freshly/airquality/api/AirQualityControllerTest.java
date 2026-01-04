package com.vertyll.freshly.airquality.api;

import com.vertyll.freshly.airquality.api.dto.*;
import com.vertyll.freshly.airquality.api.mapper.AirQualityDtoMapper;
import com.vertyll.freshly.airquality.application.AirQualityService;
import com.vertyll.freshly.airquality.application.AirQualitySyncService;
import com.vertyll.freshly.airquality.domain.*;
import com.vertyll.freshly.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AirQualityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AirQualityService airQualityService;

    @Mock
    private AirQualitySyncService syncService;

    @Mock
    private AirQualityDtoMapper dtoMapper;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private AirQualityController airQualityController;

    @BeforeEach
    void setUp() {
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Success");

        mockMvc = MockMvcBuilders.standaloneSetup(airQualityController)
                .setControllerAdvice(new AirQualityControllerAdvice(messageSource), new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("Get All Stations Tests")
    class GetAllStationsTests {

        @Test
        @DisplayName("Should get all stations successfully")
        void shouldGetAllStationsSuccessfully() throws Exception {
            // Given
            Station station1 = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
            Station station2 = new Station(124, "Station 2", "Krakow", "Street 2", 50.0647, 19.9450);
            List<Station> stations = List.of(station1, station2);

            StationResponseDto dto1 = new StationResponseDto(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
            StationResponseDto dto2 = new StationResponseDto(124, "Station 2", "Krakow", "Street 2", 50.0647, 19.9450);
            List<StationResponseDto> responseDtos = List.of(dto1, dto2);

            when(airQualityService.getAllStations()).thenReturn(stations);
            when(dtoMapper.toStationResponseList(stations)).thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(get("/air-quality/stations")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].id").value(123))
                    .andExpect(jsonPath("$.data[0].name").value("Station 1"))
                    .andExpect(jsonPath("$.data[1].id").value(124));

            verify(airQualityService).getAllStations();
            verify(dtoMapper).toStationResponseList(stations);
        }

        @Test
        @DisplayName("Should return empty list when no stations available")
        void shouldReturnEmptyListWhenNoStations() throws Exception {
            // Given
            when(airQualityService.getAllStations()).thenReturn(Collections.emptyList());
            when(dtoMapper.toStationResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/air-quality/stations")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(airQualityService).getAllStations();
        }
    }

    @Nested
    @DisplayName("Get Air Quality Index Tests")
    class GetAirQualityIndexTests {

        @Test
        @DisplayName("Should get air quality index successfully")
        void shouldGetAirQualityIndexSuccessfully() throws Exception {
            // Given
            int stationId = 123;
            AirQualityIndex index = new AirQualityIndex(
                    stationId,
                    LocalDateTime.now(ZoneOffset.UTC),
                    "Dobry",
                    "Dobry",
                    "Umiarkowany",
                    "Dobry"
            );
            AirQualityIndexResponseDto responseDto = new AirQualityIndexResponseDto(
                    stationId,
                    LocalDateTime.now(ZoneOffset.UTC),
                    "Dobry",
                    "Dobry",
                    "Umiarkowany",
                    "Dobry"
            );

            when(airQualityService.getIndexForStation(stationId)).thenReturn(Optional.of(index));
            when(dtoMapper.toAirQualityIndexResponse(index)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(get("/air-quality/stations/{stationId}/index", stationId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.stationId").value(stationId))
                    .andExpect(jsonPath("$.data.overallIndex").value("Dobry"));

            verify(airQualityService).getIndexForStation(stationId);
            verify(dtoMapper).toAirQualityIndexResponse(index);
        }

        @Test
        @DisplayName("Should throw exception when index not found")
        void shouldThrowExceptionWhenIndexNotFound() throws Exception {
            // Given
            int stationId = 999;
            when(airQualityService.getIndexForStation(stationId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/air-quality/stations/{stationId}/index", stationId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(airQualityService).getIndexForStation(stationId);
            verify(dtoMapper, never()).toAirQualityIndexResponse(any());
        }
    }

    @Nested
    @DisplayName("Get Sensor Measurements Tests")
    class GetSensorMeasurementsTests {

        @Test
        @DisplayName("Should get sensor measurements successfully")
        void shouldGetSensorMeasurementsSuccessfully() throws Exception {
            // Given
            int stationId = 123;
            SensorMeasurement.Reading reading1 = new SensorMeasurement.Reading(LocalDateTime.now(ZoneOffset.UTC), 25.5);
            SensorMeasurement.Reading reading2 = new SensorMeasurement.Reading(LocalDateTime.now(ZoneOffset.UTC).minusHours(1), 30.2);
            SensorMeasurement measurement = new SensorMeasurement(
                    1,
                    "PM10",
                    "Pył zawieszony PM10",
                    List.of(reading1, reading2)
            );
            List<SensorMeasurement> measurements = List.of(measurement);

            SensorMeasurementResponseDto responseDto = new SensorMeasurementResponseDto(
                    1,
                    "PM10",
                    "Pył zawieszony PM10",
                    List.of()
            );
            List<SensorMeasurementResponseDto> responseDtos = List.of(responseDto);

            when(airQualityService.getMeasurementsForStation(stationId)).thenReturn(measurements);
            when(dtoMapper.toSensorMeasurementResponseList(measurements)).thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(get("/air-quality/stations/{stationId}/sensors", stationId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].paramCode").value("PM10"));

            verify(airQualityService).getMeasurementsForStation(stationId);
            verify(dtoMapper).toSensorMeasurementResponseList(measurements);
        }

        @Test
        @DisplayName("Should return empty list when no measurements")
        void shouldReturnEmptyListWhenNoMeasurements() throws Exception {
            // Given
            int stationId = 123;
            when(airQualityService.getMeasurementsForStation(stationId)).thenReturn(Collections.emptyList());
            when(dtoMapper.toSensorMeasurementResponseList(Collections.emptyList())).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/air-quality/stations/{stationId}/sensors", stationId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(airQualityService).getMeasurementsForStation(stationId);
        }
    }

    @Nested
    @DisplayName("Get Latest Measurement Tests")
    class GetLatestMeasurementTests {

        @Test
        @DisplayName("Should get latest measurement successfully")
        void shouldGetLatestMeasurementSuccessfully() throws Exception {
            // Given
            int stationId = 123;
            AirQualityMeasurement measurement = new AirQualityMeasurement();
            measurement.setStationId(stationId);
            measurement.setStationName("Station 1");
            measurement.setOverallIndexLevel(AirQualityLevel.GOOD);
            measurement.setPm10Value(25.5);

            AirQualityMeasurementResponseDto responseDto = new AirQualityMeasurementResponseDto(
                    "id-123",
                    stationId,
                    "Station 1",
                    LocalDateTime.now(ZoneOffset.UTC),
                    "GOOD",
                    null,
                    null,
                    null,
                    null,
                    25.5,
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDateTime.now(ZoneOffset.UTC)
            );

            when(airQualityService.getLatestMeasurement(stationId)).thenReturn(Optional.of(measurement));
            when(dtoMapper.toAirQualityMeasurementResponse(measurement)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(get("/air-quality/stations/{stationId}/latest", stationId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.stationId").value(stationId))
                    .andExpect(jsonPath("$.data.pm10Value").value(25.5));

            verify(airQualityService).getLatestMeasurement(stationId);
            verify(dtoMapper).toAirQualityMeasurementResponse(measurement);
        }

        @Test
        @DisplayName("Should throw exception when latest measurement not found")
        void shouldThrowExceptionWhenLatestMeasurementNotFound() throws Exception {
            // Given
            int stationId = 999;
            when(airQualityService.getLatestMeasurement(stationId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/air-quality/stations/{stationId}/latest", stationId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(airQualityService).getLatestMeasurement(stationId);
            verify(dtoMapper, never()).toAirQualityMeasurementResponse(any());
        }
    }

    @Nested
    @DisplayName("Get Historical Measurements Tests")
    class GetHistoricalMeasurementsTests {

        @Test
        @DisplayName("Should get historical measurements successfully with default days")
        void shouldGetHistoricalMeasurementsWithDefaultDays() throws Exception {
            // Given
            int stationId = 123;
            List<AirQualityMeasurement> measurements = List.of(new AirQualityMeasurement());
            List<AirQualityMeasurementResponseDto> responseDtos = List.of(
                    new AirQualityMeasurementResponseDto(
                            "id-123",
                            stationId,
                            "Station 1",
                            LocalDateTime.now(ZoneOffset.UTC),
                            "GOOD",
                            null,
                            null,
                            null,
                            null,
                            25.5,
                            null,
                            null,
                            null,
                            null,
                            null,
                            LocalDateTime.now(ZoneOffset.UTC)
                    )
            );

            when(airQualityService.getHistoricalMeasurements(stationId, 7)).thenReturn(measurements);
            when(dtoMapper.toAirQualityMeasurementResponseList(measurements)).thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(get("/air-quality/stations/{stationId}/history", stationId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(airQualityService).getHistoricalMeasurements(stationId, 7);
        }

        @Test
        @DisplayName("Should get historical measurements with custom days parameter")
        void shouldGetHistoricalMeasurementsWithCustomDays() throws Exception {
            // Given
            int stationId = 123;
            int days = 30;
            List<AirQualityMeasurement> measurements = List.of(new AirQualityMeasurement());
            List<AirQualityMeasurementResponseDto> responseDtos = List.of(
                    new AirQualityMeasurementResponseDto(
                            "id-123",
                            stationId,
                            "Station 1",
                            LocalDateTime.now(ZoneOffset.UTC),
                            "GOOD",
                            null,
                            null,
                            null,
                            null,
                            25.5,
                            null,
                            null,
                            null,
                            null,
                            null,
                            LocalDateTime.now(ZoneOffset.UTC)
                    )
            );

            when(airQualityService.getHistoricalMeasurements(stationId, days)).thenReturn(measurements);
            when(dtoMapper.toAirQualityMeasurementResponseList(measurements)).thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(get("/air-quality/stations/{stationId}/history", stationId)
                            .param("days", String.valueOf(days))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));

            verify(airQualityService).getHistoricalMeasurements(stationId, days);
        }
    }

    @Nested
    @DisplayName("Trigger Sync Tests")
    class TriggerSyncTests {

        @Test
        @DisplayName("Should trigger sync successfully")
        void shouldTriggerSyncSuccessfully() throws Exception {
            // Given
            doNothing().when(syncService).triggerManualSync();

            // When & Then
            mockMvc.perform(post("/air-quality/sync/trigger")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.data").value("Synchronization triggered"));

            verify(syncService).triggerManualSync();
        }
    }

    @Nested
    @DisplayName("Get Nearest Stations Tests")
    class GetNearestStationsTests {

        @Test
        @DisplayName("Should get nearest stations with default radius")
        void shouldGetNearestStationsWithDefaultRadius() throws Exception {
            // Given
            double latitude = 52.2297;
            double longitude = 21.0122;
            Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
            StationDistance stationDistance = new StationDistance(station, 5.5);
            List<StationDistance> stationsWithDistance = List.of(stationDistance);

            StationDistanceResponseDto responseDto = new StationDistanceResponseDto(
                    new StationResponseDto(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122),
                    5.5
            );
            List<StationDistanceResponseDto> responseDtos = List.of(responseDto);

            when(airQualityService.findNearestStations(latitude, longitude, 10)).thenReturn(stationsWithDistance);
            when(dtoMapper.toStationDistanceResponseList(stationsWithDistance)).thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(get("/air-quality/stations/nearest")
                            .param("latitude", String.valueOf(latitude))
                            .param("longitude", String.valueOf(longitude))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].distanceInKm").value(5.5));

            verify(airQualityService).findNearestStations(latitude, longitude, 10);
        }

        @Test
        @DisplayName("Should get nearest stations with custom radius")
        void shouldGetNearestStationsWithCustomRadius() throws Exception {
            // Given
            double latitude = 52.2297;
            double longitude = 21.0122;
            double radius = 50;
            List<StationDistance> stationsWithDistance = List.of();

            when(airQualityService.findNearestStations(latitude, longitude, radius)).thenReturn(stationsWithDistance);
            when(dtoMapper.toStationDistanceResponseList(stationsWithDistance)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/air-quality/stations/nearest")
                            .param("latitude", String.valueOf(latitude))
                            .param("longitude", String.valueOf(longitude))
                            .param("radius", String.valueOf(radius))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(airQualityService).findNearestStations(latitude, longitude, radius);
        }
    }

    @Nested
    @DisplayName("Get Statistics Tests")
    class GetStatisticsTests {

        @Test
        @DisplayName("Should get statistics successfully")
        void shouldGetStatisticsSuccessfully() throws Exception {
            // Given
            int stationId = 123;
            AirQualityStatistics statistics = new AirQualityStatistics(
                    stationId,
                    "Station 1",
                    LocalDateTime.now(ZoneOffset.UTC).minusDays(7),
                    LocalDateTime.now(ZoneOffset.UTC),
                    100,
                    25.5, 10.0, 50.0,
                    15.2, 5.0, 30.0,
                    8.5, 12.3, 0.5, 45.2,
                    20, 30, 25, 15, 8, 2
            );
            AirQualityStatisticsResponseDto responseDto = new AirQualityStatisticsResponseDto(
                    stationId,
                    "Station 1",
                    LocalDateTime.now(ZoneOffset.UTC).minusDays(7),
                    LocalDateTime.now(ZoneOffset.UTC),
                    100,
                    new AirQualityStatisticsResponseDto.Pm10Statistics(25.5, 10.0, 50.0),
                    new AirQualityStatisticsResponseDto.Pm25Statistics(15.2, 5.0, 30.0),
                    new AirQualityStatisticsResponseDto.OtherPollutants(8.5, 12.3, 0.5, 45.2),
                    new AirQualityStatisticsResponseDto.QualityDistribution(20, 30, 25, 15, 8, 2),
                    "GOOD"
            );

            when(airQualityService.getStatistics(stationId, 7)).thenReturn(Optional.of(statistics));
            when(dtoMapper.toStatisticsResponse(statistics)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(get("/air-quality/stations/{stationId}/statistics", stationId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.stationId").value(stationId))
                    .andExpect(jsonPath("$.data.pm10.average").value(25.5));

            verify(airQualityService).getStatistics(stationId, 7);
        }

        @Test
        @DisplayName("Should throw exception when statistics not found")
        void shouldThrowExceptionWhenStatisticsNotFound() throws Exception {
            // Given
            int stationId = 999;
            when(airQualityService.getStatistics(stationId, 7)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/air-quality/stations/{stationId}/statistics", stationId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(airQualityService).getStatistics(stationId, 7);
        }
    }

    @Nested
    @DisplayName("Get Ranking Tests")
    class GetRankingTests {

        @Test
        @DisplayName("Should get ranking with default parameters")
        void shouldGetRankingWithDefaultParameters() throws Exception {
            // Given
            Station station = new Station(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
            StationRanking ranking = new StationRanking(
                    1,
                    station,
                    25.5,
                    AirQualityLevel.GOOD,
                    90
            );
            List<StationRanking> rankings = List.of(ranking);
            StationResponseDto stationDto = new StationResponseDto(123, "Station 1", "Warsaw", "Street 1", 52.2297, 21.0122);
            StationRankingResponseDto responseDto = new StationRankingResponseDto(
                    1,
                    stationDto,
                    25.5,
                    "GOOD",
                    90
            );
            List<StationRankingResponseDto> responseDtos = List.of(responseDto);

            when(airQualityService.getRanking(7, 10)).thenReturn(rankings);
            when(dtoMapper.toRankingResponseList(rankings)).thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(get("/air-quality/stations/ranking")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].station.id").value(123));

            verify(airQualityService).getRanking(7, 10);
        }

        @Test
        @DisplayName("Should get ranking with custom parameters")
        void shouldGetRankingWithCustomParameters() throws Exception {
            // Given
            int days = 30;
            int limit = 20;
            List<StationRanking> rankings = List.of();

            when(airQualityService.getRanking(days, limit)).thenReturn(rankings);
            when(dtoMapper.toRankingResponseList(rankings)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/air-quality/stations/ranking")
                            .param("days", String.valueOf(days))
                            .param("limit", String.valueOf(limit))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(airQualityService).getRanking(days, limit);
        }
    }
}
