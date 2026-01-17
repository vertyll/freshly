package com.vertyll.freshly.airquality.api;

import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import com.vertyll.freshly.airquality.api.dto.*;
import com.vertyll.freshly.airquality.api.mapper.AirQualityDtoMapper;
import com.vertyll.freshly.airquality.application.AirQualityService;
import com.vertyll.freshly.airquality.application.AirQualitySyncService;
import com.vertyll.freshly.airquality.domain.AirQualityMeasurement;
import com.vertyll.freshly.airquality.domain.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.Station;
import com.vertyll.freshly.airquality.domain.StationDistance;
import com.vertyll.freshly.airquality.domain.StationRanking;
import com.vertyll.freshly.airquality.domain.exception.AirQualityDataNotFoundException;
import com.vertyll.freshly.common.response.ApiResponse;

@RestController
@RequestMapping("/air-quality")
@RequiredArgsConstructor
public class AirQualityController {

    private static final String SUCCESS_STATIONS_FETCHED_MSG_KEY =
            "success.airquality.stationsFetched";
    private static final String SUCCESS_INDEX_FETCHED_MSG_KEY = "success.airquality.indexFetched";
    private static final String SUCCESS_MEASUREMENTS_FETCHED_MSG_KEY =
            "success.airquality.measurementsFetched";
    private static final String SUCCESS_LATEST_FETCHED_MSG_KEY = "success.airquality.latestFetched";
    private static final String SUCCESS_HISTORY_FETCHED_MSG_KEY =
            "success.airquality.historyFetched";
    private static final String SUCCESS_SYNC_TRIGGERED_MSG_KEY = "success.airquality.syncTriggered";
    private static final String SUCCESS_NEAREST_FETCHED_MSG_KEY =
            "success.airquality.nearestFetched";
    private static final String SUCCESS_STATISTICS_FETCHED_MSG_KEY =
            "success.airquality.statisticsFetched";
    private static final String SUCCESS_RANKING_FETCHED_MSG_KEY =
            "success.airquality.rankingFetched";

    private static final String SYNC_TRIGGERED = "Synchronization triggered";
    private static final String DEFAULT_DAYS = "7";
    private static final String DEFAULT_RADIUS = "10";
    private static final String DEFAULT_LIMIT = "10";

    private final AirQualityService airQualityService;
    private final AirQualitySyncService syncService;
    private final AirQualityDtoMapper dtoMapper;
    private final MessageSource messageSource;

    /** Get list of all air quality stations */
    @GetMapping("/stations")
    public ResponseEntity<ApiResponse<List<StationResponseDto>>> getStations() {
        List<Station> stations = airQualityService.getAllStations();
        List<StationResponseDto> response = dtoMapper.toStationResponseList(stations);
        return ApiResponse.buildResponse(
                response, SUCCESS_STATIONS_FETCHED_MSG_KEY, messageSource, HttpStatus.OK);
    }

    /** Get current air quality index (live from GIOŚ API) */
    @GetMapping("/stations/{stationId}/index")
    public ResponseEntity<ApiResponse<AirQualityIndexResponseDto>> getIndex(
            @PathVariable int stationId) {
        return airQualityService
                .getIndexForStation(stationId)
                .map(
                        index -> {
                            AirQualityIndexResponseDto response =
                                    dtoMapper.toAirQualityIndexResponse(index);
                            return ApiResponse.buildResponse(
                                    response,
                                    SUCCESS_INDEX_FETCHED_MSG_KEY,
                                    messageSource,
                                    HttpStatus.OK);
                        })
                .orElseThrow(() -> new AirQualityDataNotFoundException(stationId));
    }

    /** Get current sensor measurements (live from GIOŚ API) */
    @GetMapping("/stations/{stationId}/sensors")
    public ResponseEntity<ApiResponse<List<SensorMeasurementResponseDto>>> getMeasurements(
            @PathVariable int stationId) {
        List<SensorMeasurement> measurements =
                airQualityService.getMeasurementsForStation(stationId);
        List<SensorMeasurementResponseDto> response =
                dtoMapper.toSensorMeasurementResponseList(measurements);
        return ApiResponse.buildResponse(
                response, SUCCESS_MEASUREMENTS_FETCHED_MSG_KEY, messageSource, HttpStatus.OK);
    }

    /**
     * Get latest cached measurement from database Use this for quick access without hitting GIOŚ
     * API
     */
    @GetMapping("/stations/{stationId}/latest")
    public ResponseEntity<ApiResponse<AirQualityMeasurementResponseDto>> getLatestMeasurement(
            @PathVariable int stationId) {
        return airQualityService
                .getLatestMeasurement(stationId)
                .map(
                        measurement -> {
                            AirQualityMeasurementResponseDto response =
                                    dtoMapper.toAirQualityMeasurementResponse(measurement);
                            return ApiResponse.buildResponse(
                                    response,
                                    SUCCESS_LATEST_FETCHED_MSG_KEY,
                                    messageSource,
                                    HttpStatus.OK);
                        })
                .orElseThrow(() -> new AirQualityDataNotFoundException(stationId));
    }

    /**
     * Get historical measurements for charts
     *
     * @param stationId Station ID
     * @param days Number of days to look back (default 7, max 90)
     */
    @GetMapping("/stations/{stationId}/history")
    public ResponseEntity<ApiResponse<List<AirQualityMeasurementResponseDto>>> getHistory(
            @PathVariable int stationId, @RequestParam(defaultValue = DEFAULT_DAYS) int days) {
        List<AirQualityMeasurement> history =
                airQualityService.getHistoricalMeasurements(stationId, days);
        List<AirQualityMeasurementResponseDto> response =
                dtoMapper.toAirQualityMeasurementResponseList(history);
        return ApiResponse.buildResponse(
                response, SUCCESS_HISTORY_FETCHED_MSG_KEY, messageSource, HttpStatus.OK);
    }

    /**
     * Manually trigger data synchronization (for admin/testing) In production, consider
     * adding @PreAuthorize("hasRole('ADMIN')")
     */
    @PostMapping("/sync/trigger")
    public ResponseEntity<ApiResponse<String>> triggerSync() {
        syncService.triggerManualSync();
        return ApiResponse.buildResponse(
                SYNC_TRIGGERED, SUCCESS_SYNC_TRIGGERED_MSG_KEY, messageSource, HttpStatus.ACCEPTED);
    }

    /**
     * Find nearest stations to given coordinates
     *
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param radius Search radius in kilometers (default 10, max 100)
     */
    @GetMapping("/stations/nearest")
    public ResponseEntity<ApiResponse<List<StationDistanceResponseDto>>> getNearestStations(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = DEFAULT_RADIUS) double radius) {
        List<StationDistance> stationsWithDistance =
                airQualityService.findNearestStations(latitude, longitude, radius);
        List<StationDistanceResponseDto> response =
                dtoMapper.toStationDistanceResponseList(stationsWithDistance);
        return ApiResponse.buildResponse(
                response, SUCCESS_NEAREST_FETCHED_MSG_KEY, messageSource, HttpStatus.OK);
    }

    /**
     * Get statistics for a station within time range
     *
     * @param stationId Station ID
     * @param days Number of days to analyze (default 7, max 90)
     */
    @GetMapping("/stations/{stationId}/statistics")
    public ResponseEntity<ApiResponse<AirQualityStatisticsResponseDto>> getStatistics(
            @PathVariable int stationId, @RequestParam(defaultValue = DEFAULT_DAYS) int days) {
        return airQualityService
                .getStatistics(stationId, days)
                .map(
                        stats -> {
                            AirQualityStatisticsResponseDto response =
                                    dtoMapper.toStatisticsResponse(stats);
                            return ApiResponse.buildResponse(
                                    response,
                                    SUCCESS_STATISTICS_FETCHED_MSG_KEY,
                                    messageSource,
                                    HttpStatus.OK);
                        })
                .orElseThrow(() -> new AirQualityDataNotFoundException(stationId));
    }

    /**
     * Get ranking of stations (best to worst air quality)
     *
     * @param days Number of days to analyze (default 7, max 90)
     * @param limit Max number of stations to return (default 10, max 50)
     */
    @GetMapping("/stations/ranking")
    public ResponseEntity<ApiResponse<List<StationRankingResponseDto>>> getRanking(
            @RequestParam(defaultValue = DEFAULT_DAYS) int days,
            @RequestParam(defaultValue = DEFAULT_LIMIT) int limit) {
        List<StationRanking> rankings = airQualityService.getRanking(days, limit);
        List<StationRankingResponseDto> response = dtoMapper.toRankingResponseList(rankings);
        return ApiResponse.buildResponse(
                response, SUCCESS_RANKING_FETCHED_MSG_KEY, messageSource, HttpStatus.OK);
    }
}
