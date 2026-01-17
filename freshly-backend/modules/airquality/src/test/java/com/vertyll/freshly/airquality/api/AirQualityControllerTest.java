package com.vertyll.freshly.airquality.api;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

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

import com.vertyll.freshly.airquality.api.dto.*;
import com.vertyll.freshly.airquality.api.mapper.AirQualityDtoMapper;
import com.vertyll.freshly.airquality.application.AirQualityService;
import com.vertyll.freshly.airquality.application.AirQualitySyncService;
import com.vertyll.freshly.airquality.domain.*;
import com.vertyll.freshly.common.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AirQualityControllerTest {

    private static final String ENDPOINT_STATIONS = "/air-quality/stations";
    private static final String ENDPOINT_STATION_INDEX = "/air-quality/stations/{stationId}/index";
    private static final String ENDPOINT_STATION_SENSORS =
            "/air-quality/stations/{stationId}/sensors";
    private static final String ENDPOINT_STATION_LATEST =
            "/air-quality/stations/{stationId}/latest";
    private static final String ENDPOINT_STATION_HISTORY =
            "/air-quality/stations/{stationId}/history";
    private static final String ENDPOINT_SYNC_TRIGGER = "/air-quality/sync/trigger";
    private static final String ENDPOINT_STATIONS_NEAREST = "/air-quality/stations/nearest";
    private static final String ENDPOINT_STATION_STATISTICS =
            "/air-quality/stations/{stationId}/statistics";
    private static final String ENDPOINT_STATIONS_RANKING = "/air-quality/stations/ranking";

    private static final String PARAM_DAYS = "days";
    private static final String PARAM_LATITUDE = "latitude";
    private static final String PARAM_LONGITUDE = "longitude";
    private static final String PARAM_RADIUS = "radius";
    private static final String PARAM_LIMIT = "limit";

    private static final String JSON_PATH_DATA = "$.data";
    private static final String JSON_PATH_DATA_0_ID = "$.data[0].id";
    private static final String JSON_PATH_DATA_0_NAME = "$.data[0].name";
    private static final String JSON_PATH_DATA_1_ID = "$.data[1].id";
    private static final String JSON_PATH_DATA_STATION_ID = "$.data.stationId";
    private static final String JSON_PATH_DATA_OVERALL_INDEX = "$.data.overallIndex";
    private static final String JSON_PATH_DATA_0_PARAM_CODE = "$.data[0].paramCode";
    private static final String JSON_PATH_DATA_PM10_VALUE = "$.data.pm10Value";
    private static final String JSON_PATH_DATA_0_DISTANCE = "$.data[0].distanceInKm";
    private static final String JSON_PATH_DATA_PM10_AVERAGE = "$.data.pm10.average";
    private static final String JSON_PATH_DATA_0_STATION_ID = "$.data[0].station.id";

    private static final String STATION_1_NAME = "Station 1";
    private static final String STATION_2_NAME = "Station 2";
    private static final String CITY_WARSAW = "Warsaw";
    private static final String CITY_KRAKOW = "Krakow";
    private static final String STREET_1 = "Street 1";
    private static final String STREET_2 = "Street 2";
    private static final String QUALITY_GOOD_PL = "Dobry";
    private static final String QUALITY_MODERATE_PL = "Umiarkowany";
    private static final String QUALITY_GOOD_EN = "GOOD";
    private static final String PARAM_PM10 = "PM10";
    private static final String PARAM_PM10_NAME = "Pył zawieszony PM10";
    private static final String SYNC_TRIGGERED_MESSAGE = "Synchronization triggered";
    private static final String MESSAGE_SUCCESS = "Success";
    private static final String MEASUREMENT_ID = "id-123";

    private static final int STATION_ID_123 = 123;
    private static final int STATION_ID_124 = 124;
    private static final int STATION_ID_999 = 999;
    private static final int SENSOR_ID_1 = 1;
    private static final int DEFAULT_DAYS = 7;
    private static final int CUSTOM_DAYS_30 = 30;
    private static final int DEFAULT_RADIUS = 10;
    private static final int CUSTOM_RADIUS_50 = 50;
    private static final int DEFAULT_LIMIT = 10;
    private static final int CUSTOM_LIMIT_20 = 20;
    private static final int MEASUREMENT_COUNT_100 = 100;
    private static final int RANK_1 = 1;
    private static final int MEASUREMENT_COUNT_90 = 90;

    private static final double LAT_WARSAW = 52.2297;
    private static final double LON_WARSAW = 21.0122;
    private static final double LAT_KRAKOW = 50.0647;
    private static final double LON_KRAKOW = 19.9450;
    private static final double PM10_VALUE_25_5 = 25.5;
    private static final double PM10_VALUE_30_2 = 30.2;
    private static final double DISTANCE_5_5_KM = 5.5;
    private static final double PM10_MIN_10_0 = 10.0;
    private static final double PM10_MAX_50_0 = 50.0;
    private static final double PM25_AVG_15_2 = 15.2;
    private static final double PM25_MIN_5_0 = 5.0;
    private static final double PM25_MAX_30_0 = 30.0;
    private static final double SO2_AVG_8_5 = 8.5;
    private static final double NO2_AVG_12_3 = 12.3;
    private static final double CO_AVG_0_5 = 0.5;
    private static final double O3_AVG_45_2 = 45.2;

    private static final int QUALITY_VERY_GOOD_20 = 20;
    private static final int QUALITY_GOOD_30 = 30;
    private static final int QUALITY_MODERATE_25 = 25;
    private static final int QUALITY_SUFFICIENT_15 = 15;
    private static final int QUALITY_BAD_8 = 8;
    private static final int QUALITY_VERY_BAD_2 = 2;

    private MockMvc mockMvc;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private AirQualityService airQualityService;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private AirQualitySyncService syncService;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private AirQualityDtoMapper dtoMapper;

    @Mock
    @SuppressWarnings("NullAway.Init")
    private MessageSource messageSource;

    @InjectMocks private AirQualityController airQualityController;

    @BeforeEach
    @SuppressWarnings("NullAway.Init")
    void setUp() {
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn(MESSAGE_SUCCESS);

        mockMvc =
                MockMvcBuilders.standaloneSetup(airQualityController)
                        .setControllerAdvice(
                                new AirQualityControllerAdvice(messageSource),
                                new GlobalExceptionHandler())
                        .build();
    }

    @Nested
    @DisplayName("Get All Stations Tests")
    class GetAllStationsTests {

        @Test
        @DisplayName("Should get all stations successfully")
        void shouldGetAllStationsSuccessfully() throws Exception {
            // Given
            Station station1 =
                    new Station(
                            STATION_ID_123,
                            STATION_1_NAME,
                            CITY_WARSAW,
                            STREET_1,
                            LAT_WARSAW,
                            LON_WARSAW);
            Station station2 =
                    new Station(
                            STATION_ID_124,
                            STATION_2_NAME,
                            CITY_KRAKOW,
                            STREET_2,
                            LAT_KRAKOW,
                            LON_KRAKOW);
            List<Station> stations = List.of(station1, station2);

            StationResponseDto dto1 =
                    new StationResponseDto(
                            STATION_ID_123,
                            STATION_1_NAME,
                            CITY_WARSAW,
                            STREET_1,
                            LAT_WARSAW,
                            LON_WARSAW);
            StationResponseDto dto2 =
                    new StationResponseDto(
                            STATION_ID_124,
                            STATION_2_NAME,
                            CITY_KRAKOW,
                            STREET_2,
                            LAT_KRAKOW,
                            LON_KRAKOW);
            List<StationResponseDto> responseDtos = List.of(dto1, dto2);

            when(airQualityService.getAllStations()).thenReturn(stations);
            when(dtoMapper.toStationResponseList(stations)).thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(get(ENDPOINT_STATIONS).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA, hasSize(2)))
                    .andExpect(jsonPath(JSON_PATH_DATA_0_ID).value(STATION_ID_123))
                    .andExpect(jsonPath(JSON_PATH_DATA_0_NAME).value(STATION_1_NAME))
                    .andExpect(jsonPath(JSON_PATH_DATA_1_ID).value(STATION_ID_124));

            verify(airQualityService).getAllStations();
            verify(dtoMapper).toStationResponseList(stations);
        }

        @Test
        @DisplayName("Should return empty list when no stations available")
        void shouldReturnEmptyListWhenNoStations() throws Exception {
            // Given
            when(airQualityService.getAllStations()).thenReturn(Collections.emptyList());
            when(dtoMapper.toStationResponseList(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(ENDPOINT_STATIONS).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA, hasSize(0)));

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
            AirQualityIndex index =
                    new AirQualityIndex(
                            STATION_ID_123,
                            LocalDateTime.now(ZoneOffset.UTC),
                            QUALITY_GOOD_PL,
                            QUALITY_GOOD_PL,
                            QUALITY_MODERATE_PL,
                            QUALITY_GOOD_PL);
            AirQualityIndexResponseDto responseDto =
                    new AirQualityIndexResponseDto(
                            STATION_ID_123,
                            LocalDateTime.now(ZoneOffset.UTC),
                            QUALITY_GOOD_PL,
                            QUALITY_GOOD_PL,
                            QUALITY_MODERATE_PL,
                            QUALITY_GOOD_PL);

            when(airQualityService.getIndexForStation(STATION_ID_123))
                    .thenReturn(Optional.of(index));
            when(dtoMapper.toAirQualityIndexResponse(index)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATION_INDEX, STATION_ID_123)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA_STATION_ID).value(STATION_ID_123))
                    .andExpect(jsonPath(JSON_PATH_DATA_OVERALL_INDEX).value(QUALITY_GOOD_PL));

            verify(airQualityService).getIndexForStation(STATION_ID_123);
            verify(dtoMapper).toAirQualityIndexResponse(index);
        }

        @Test
        @DisplayName("Should throw exception when index not found")
        void shouldThrowExceptionWhenIndexNotFound() throws Exception {
            // Given
            when(airQualityService.getIndexForStation(STATION_ID_999)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATION_INDEX, STATION_ID_999)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(airQualityService).getIndexForStation(STATION_ID_999);
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
            SensorMeasurement.Reading reading1 =
                    new SensorMeasurement.Reading(
                            LocalDateTime.now(ZoneOffset.UTC), PM10_VALUE_25_5);
            SensorMeasurement.Reading reading2 =
                    new SensorMeasurement.Reading(
                            LocalDateTime.now(ZoneOffset.UTC).minusHours(1), PM10_VALUE_30_2);
            SensorMeasurement measurement =
                    new SensorMeasurement(
                            SENSOR_ID_1, PARAM_PM10, PARAM_PM10_NAME, List.of(reading1, reading2));
            List<SensorMeasurement> measurements = List.of(measurement);

            SensorMeasurementResponseDto responseDto =
                    new SensorMeasurementResponseDto(
                            SENSOR_ID_1, PARAM_PM10, PARAM_PM10_NAME, List.of());
            List<SensorMeasurementResponseDto> responseDtos = List.of(responseDto);

            when(airQualityService.getMeasurementsForStation(STATION_ID_123))
                    .thenReturn(measurements);
            when(dtoMapper.toSensorMeasurementResponseList(measurements)).thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATION_SENSORS, STATION_ID_123)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA, hasSize(1)))
                    .andExpect(jsonPath(JSON_PATH_DATA_0_PARAM_CODE).value(PARAM_PM10));

            verify(airQualityService).getMeasurementsForStation(STATION_ID_123);
            verify(dtoMapper).toSensorMeasurementResponseList(measurements);
        }

        @Test
        @DisplayName("Should return empty list when no measurements")
        void shouldReturnEmptyListWhenNoMeasurements() throws Exception {
            // Given
            when(airQualityService.getMeasurementsForStation(STATION_ID_123))
                    .thenReturn(Collections.emptyList());
            when(dtoMapper.toSensorMeasurementResponseList(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATION_SENSORS, STATION_ID_123)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA, hasSize(0)));

            verify(airQualityService).getMeasurementsForStation(STATION_ID_123);
        }
    }

    @Nested
    @DisplayName("Get Latest Measurement Tests")
    class GetLatestMeasurementTests {

        @Test
        @DisplayName("Should get latest measurement successfully")
        void shouldGetLatestMeasurementSuccessfully() throws Exception {
            // Given
            AirQualityMeasurement measurement = new AirQualityMeasurement();
            measurement.setStationId(STATION_ID_123);
            measurement.setStationName(STATION_1_NAME);
            measurement.setOverallIndexLevel(AirQualityLevel.GOOD);
            measurement.setPm10Value(PM10_VALUE_25_5);

            AirQualityMeasurementResponseDto responseDto =
                    new AirQualityMeasurementResponseDto(
                            MEASUREMENT_ID,
                            STATION_ID_123,
                            STATION_1_NAME,
                            LocalDateTime.now(ZoneOffset.UTC),
                            QUALITY_GOOD_EN,
                            null,
                            null,
                            null,
                            null,
                            PM10_VALUE_25_5,
                            null,
                            null,
                            null,
                            null,
                            null,
                            LocalDateTime.now(ZoneOffset.UTC));

            when(airQualityService.getLatestMeasurement(STATION_ID_123))
                    .thenReturn(Optional.of(measurement));
            when(dtoMapper.toAirQualityMeasurementResponse(measurement)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATION_LATEST, STATION_ID_123)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA_STATION_ID).value(STATION_ID_123))
                    .andExpect(jsonPath(JSON_PATH_DATA_PM10_VALUE).value(PM10_VALUE_25_5));

            verify(airQualityService).getLatestMeasurement(STATION_ID_123);
            verify(dtoMapper).toAirQualityMeasurementResponse(measurement);
        }

        @Test
        @DisplayName("Should throw exception when latest measurement not found")
        void shouldThrowExceptionWhenLatestMeasurementNotFound() throws Exception {
            // Given
            when(airQualityService.getLatestMeasurement(STATION_ID_999))
                    .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATION_LATEST, STATION_ID_999)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(airQualityService).getLatestMeasurement(STATION_ID_999);
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
            List<AirQualityMeasurement> measurements = List.of(new AirQualityMeasurement());
            List<AirQualityMeasurementResponseDto> responseDtos =
                    List.of(
                            new AirQualityMeasurementResponseDto(
                                    MEASUREMENT_ID,
                                    STATION_ID_123,
                                    STATION_1_NAME,
                                    LocalDateTime.now(ZoneOffset.UTC),
                                    QUALITY_GOOD_EN,
                                    null,
                                    null,
                                    null,
                                    null,
                                    PM10_VALUE_25_5,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    LocalDateTime.now(ZoneOffset.UTC)));

            when(airQualityService.getHistoricalMeasurements(STATION_ID_123, DEFAULT_DAYS))
                    .thenReturn(measurements);
            when(dtoMapper.toAirQualityMeasurementResponseList(measurements))
                    .thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATION_HISTORY, STATION_ID_123)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA, hasSize(1)));

            verify(airQualityService).getHistoricalMeasurements(STATION_ID_123, DEFAULT_DAYS);
        }

        @Test
        @DisplayName("Should get historical measurements with custom days parameter")
        void shouldGetHistoricalMeasurementsWithCustomDays() throws Exception {
            // Given
            List<AirQualityMeasurement> measurements = List.of(new AirQualityMeasurement());
            List<AirQualityMeasurementResponseDto> responseDtos =
                    List.of(
                            new AirQualityMeasurementResponseDto(
                                    MEASUREMENT_ID,
                                    STATION_ID_123,
                                    STATION_1_NAME,
                                    LocalDateTime.now(ZoneOffset.UTC),
                                    QUALITY_GOOD_EN,
                                    null,
                                    null,
                                    null,
                                    null,
                                    PM10_VALUE_25_5,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    LocalDateTime.now(ZoneOffset.UTC)));

            when(airQualityService.getHistoricalMeasurements(STATION_ID_123, CUSTOM_DAYS_30))
                    .thenReturn(measurements);
            when(dtoMapper.toAirQualityMeasurementResponseList(measurements))
                    .thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATION_HISTORY, STATION_ID_123)
                                    .param(PARAM_DAYS, String.valueOf(CUSTOM_DAYS_30))
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA, hasSize(1)));

            verify(airQualityService).getHistoricalMeasurements(STATION_ID_123, CUSTOM_DAYS_30);
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
            mockMvc.perform(post(ENDPOINT_SYNC_TRIGGER).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath(JSON_PATH_DATA).value(SYNC_TRIGGERED_MESSAGE));

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
            Station station =
                    new Station(
                            STATION_ID_123,
                            STATION_1_NAME,
                            CITY_WARSAW,
                            STREET_1,
                            LAT_WARSAW,
                            LON_WARSAW);
            StationDistance stationDistance = new StationDistance(station, DISTANCE_5_5_KM);
            List<StationDistance> stationsWithDistance = List.of(stationDistance);

            StationDistanceResponseDto responseDto =
                    new StationDistanceResponseDto(
                            new StationResponseDto(
                                    STATION_ID_123,
                                    STATION_1_NAME,
                                    CITY_WARSAW,
                                    STREET_1,
                                    LAT_WARSAW,
                                    LON_WARSAW),
                            DISTANCE_5_5_KM);
            List<StationDistanceResponseDto> responseDtos = List.of(responseDto);

            when(airQualityService.findNearestStations(LAT_WARSAW, LON_WARSAW, DEFAULT_RADIUS))
                    .thenReturn(stationsWithDistance);
            when(dtoMapper.toStationDistanceResponseList(stationsWithDistance))
                    .thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATIONS_NEAREST)
                                    .param(PARAM_LATITUDE, String.valueOf(LAT_WARSAW))
                                    .param(PARAM_LONGITUDE, String.valueOf(LON_WARSAW))
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA, hasSize(1)))
                    .andExpect(jsonPath(JSON_PATH_DATA_0_DISTANCE).value(DISTANCE_5_5_KM));

            verify(airQualityService).findNearestStations(LAT_WARSAW, LON_WARSAW, DEFAULT_RADIUS);
        }

        @Test
        @DisplayName("Should get nearest stations with custom radius")
        void shouldGetNearestStationsWithCustomRadius() throws Exception {
            // Given
            List<StationDistance> stationsWithDistance = List.of();

            when(airQualityService.findNearestStations(LAT_WARSAW, LON_WARSAW, CUSTOM_RADIUS_50))
                    .thenReturn(stationsWithDistance);
            when(dtoMapper.toStationDistanceResponseList(stationsWithDistance))
                    .thenReturn(List.of());

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATIONS_NEAREST)
                                    .param(PARAM_LATITUDE, String.valueOf(LAT_WARSAW))
                                    .param(PARAM_LONGITUDE, String.valueOf(LON_WARSAW))
                                    .param(PARAM_RADIUS, String.valueOf(CUSTOM_RADIUS_50))
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA, hasSize(0)));

            verify(airQualityService).findNearestStations(LAT_WARSAW, LON_WARSAW, CUSTOM_RADIUS_50);
        }
    }

    @Nested
    @DisplayName("Get Statistics Tests")
    class GetStatisticsTests {

        @Test
        @DisplayName("Should get statistics successfully")
        void shouldGetStatisticsSuccessfully() throws Exception {
            // Given
            AirQualityStatistics statistics =
                    new AirQualityStatistics(
                            STATION_ID_123,
                            STATION_1_NAME,
                            LocalDateTime.now(ZoneOffset.UTC).minusDays(DEFAULT_DAYS),
                            LocalDateTime.now(ZoneOffset.UTC),
                            MEASUREMENT_COUNT_100,
                            PM10_VALUE_25_5,
                            PM10_MIN_10_0,
                            PM10_MAX_50_0,
                            PM25_AVG_15_2,
                            PM25_MIN_5_0,
                            PM25_MAX_30_0,
                            SO2_AVG_8_5,
                            NO2_AVG_12_3,
                            CO_AVG_0_5,
                            O3_AVG_45_2,
                            QUALITY_VERY_GOOD_20,
                            QUALITY_GOOD_30,
                            QUALITY_MODERATE_25,
                            QUALITY_SUFFICIENT_15,
                            QUALITY_BAD_8,
                            QUALITY_VERY_BAD_2);
            AirQualityStatisticsResponseDto responseDto =
                    new AirQualityStatisticsResponseDto(
                            STATION_ID_123,
                            STATION_1_NAME,
                            LocalDateTime.now(ZoneOffset.UTC).minusDays(DEFAULT_DAYS),
                            LocalDateTime.now(ZoneOffset.UTC),
                            MEASUREMENT_COUNT_100,
                            new AirQualityStatisticsResponseDto.Pm10Statistics(
                                    PM10_VALUE_25_5, PM10_MIN_10_0, PM10_MAX_50_0),
                            new AirQualityStatisticsResponseDto.Pm25Statistics(
                                    PM25_AVG_15_2, PM25_MIN_5_0, PM25_MAX_30_0),
                            new AirQualityStatisticsResponseDto.OtherPollutants(
                                    SO2_AVG_8_5, NO2_AVG_12_3, CO_AVG_0_5, O3_AVG_45_2),
                            new AirQualityStatisticsResponseDto.QualityDistribution(
                                    QUALITY_VERY_GOOD_20,
                                    QUALITY_GOOD_30,
                                    QUALITY_MODERATE_25,
                                    QUALITY_SUFFICIENT_15,
                                    QUALITY_BAD_8,
                                    QUALITY_VERY_BAD_2),
                            QUALITY_GOOD_EN);

            when(airQualityService.getStatistics(STATION_ID_123, DEFAULT_DAYS))
                    .thenReturn(Optional.of(statistics));
            when(dtoMapper.toStatisticsResponse(statistics)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATION_STATISTICS, STATION_ID_123)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA_STATION_ID).value(STATION_ID_123))
                    .andExpect(jsonPath(JSON_PATH_DATA_PM10_AVERAGE).value(PM10_VALUE_25_5));

            verify(airQualityService).getStatistics(STATION_ID_123, DEFAULT_DAYS);
        }

        @Test
        @DisplayName("Should throw exception when statistics not found")
        void shouldThrowExceptionWhenStatisticsNotFound() throws Exception {
            // Given
            when(airQualityService.getStatistics(STATION_ID_999, DEFAULT_DAYS))
                    .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATION_STATISTICS, STATION_ID_999)
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(airQualityService).getStatistics(STATION_ID_999, DEFAULT_DAYS);
        }
    }

    @Nested
    @DisplayName("Get Ranking Tests")
    class GetRankingTests {

        @Test
        @DisplayName("Should get ranking with default parameters")
        void shouldGetRankingWithDefaultParameters() throws Exception {
            // Given
            Station station =
                    new Station(
                            STATION_ID_123,
                            STATION_1_NAME,
                            CITY_WARSAW,
                            STREET_1,
                            LAT_WARSAW,
                            LON_WARSAW);
            StationRanking ranking =
                    new StationRanking(
                            RANK_1,
                            station,
                            PM10_VALUE_25_5,
                            AirQualityLevel.GOOD,
                            MEASUREMENT_COUNT_90);
            List<StationRanking> rankings = List.of(ranking);
            StationResponseDto stationDto =
                    new StationResponseDto(
                            STATION_ID_123,
                            STATION_1_NAME,
                            CITY_WARSAW,
                            STREET_1,
                            LAT_WARSAW,
                            LON_WARSAW);
            StationRankingResponseDto responseDto =
                    new StationRankingResponseDto(
                            RANK_1,
                            stationDto,
                            PM10_VALUE_25_5,
                            QUALITY_GOOD_EN,
                            MEASUREMENT_COUNT_90);
            List<StationRankingResponseDto> responseDtos = List.of(responseDto);

            when(airQualityService.getRanking(DEFAULT_DAYS, DEFAULT_LIMIT)).thenReturn(rankings);
            when(dtoMapper.toRankingResponseList(rankings)).thenReturn(responseDtos);

            // When & Then
            mockMvc.perform(get(ENDPOINT_STATIONS_RANKING).contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA, hasSize(1)))
                    .andExpect(jsonPath(JSON_PATH_DATA_0_STATION_ID).value(STATION_ID_123));

            verify(airQualityService).getRanking(DEFAULT_DAYS, DEFAULT_LIMIT);
        }

        @Test
        @DisplayName("Should get ranking with custom parameters")
        void shouldGetRankingWithCustomParameters() throws Exception {
            // Given
            List<StationRanking> rankings = List.of();

            when(airQualityService.getRanking(CUSTOM_DAYS_30, CUSTOM_LIMIT_20))
                    .thenReturn(rankings);
            when(dtoMapper.toRankingResponseList(rankings)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(
                            get(ENDPOINT_STATIONS_RANKING)
                                    .param(PARAM_DAYS, String.valueOf(CUSTOM_DAYS_30))
                                    .param(PARAM_LIMIT, String.valueOf(CUSTOM_LIMIT_20))
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_PATH_DATA, hasSize(0)));

            verify(airQualityService).getRanking(CUSTOM_DAYS_30, CUSTOM_LIMIT_20);
        }
    }
}
