package com.vertyll.freshly.airquality.api;

import com.vertyll.freshly.airquality.api.dto.*;
import com.vertyll.freshly.airquality.api.mapper.AirQualityDtoMapper;
import com.vertyll.freshly.airquality.application.AirQualityService;
import com.vertyll.freshly.airquality.application.AirQualitySyncService;
import com.vertyll.freshly.airquality.domain.exception.AirQualityDataNotFoundException;
import com.vertyll.freshly.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/air-quality")
@RequiredArgsConstructor
public class AirQualityController {

    private final AirQualityService airQualityService;
    private final AirQualitySyncService syncService;
    private final AirQualityDtoMapper dtoMapper;
    private final MessageSource messageSource;

    /**
     * Get list of all air quality stations
     */
    @GetMapping("/stations")
    public ResponseEntity<ApiResponse<List<StationResponseDto>>> getStations() {
        var stations = airQualityService.getAllStations();
        var response = dtoMapper.toStationResponseList(stations);
        return ApiResponse.buildResponse(response, "success.airquality.stationsFetched", messageSource, HttpStatus.OK);
    }

    /**
     * Get current air quality index (live from GIOŚ API)
     */
    @GetMapping("/stations/{stationId}/index")
    public ResponseEntity<ApiResponse<AirQualityIndexResponseDto>> getIndex(@PathVariable int stationId) {
        return airQualityService.getIndexForStation(stationId)
                .map(index -> {
                    var response = dtoMapper.toAirQualityIndexResponse(index);
                    return ApiResponse.buildResponse(response, "success.airquality.indexFetched", messageSource, HttpStatus.OK);
                })
                .orElseThrow(() -> new AirQualityDataNotFoundException(stationId));
    }

    /**
     * Get current sensor measurements (live from GIOŚ API)
     */
    @GetMapping("/stations/{stationId}/sensors")
    public ResponseEntity<ApiResponse<List<SensorMeasurementResponseDto>>> getMeasurements(@PathVariable int stationId) {
        var measurements = airQualityService.getMeasurementsForStation(stationId);
        var response = dtoMapper.toSensorMeasurementResponseList(measurements);
        return ApiResponse.buildResponse(response, "success.airquality.measurementsFetched", messageSource, HttpStatus.OK);
    }

    /**
     * Get latest cached measurement from database
     * Use this for quick access without hitting GIOŚ API
     */
    @GetMapping("/stations/{stationId}/latest")
    public ResponseEntity<ApiResponse<AirQualityMeasurementResponseDto>> getLatestMeasurement(@PathVariable int stationId) {
        return airQualityService.getLatestMeasurement(stationId)
                .map(measurement -> {
                    var response = dtoMapper.toAirQualityMeasurementResponse(measurement);
                    return ApiResponse.buildResponse(response, "success.airquality.latestFetched", messageSource, HttpStatus.OK);
                })
                .orElseThrow(() -> new AirQualityDataNotFoundException(stationId));
    }

    /**
     * Get historical measurements for charts
     *
     * @param stationId Station ID
     * @param days      Number of days to look back (default 7, max 90)
     */
    @GetMapping("/stations/{stationId}/history")
    public ResponseEntity<ApiResponse<List<AirQualityMeasurementResponseDto>>> getHistory(
            @PathVariable int stationId,
            @RequestParam(defaultValue = "7") int days
    ) {
        var history = airQualityService.getHistoricalMeasurements(stationId, days);
        var response = dtoMapper.toAirQualityMeasurementResponseList(history);
        return ApiResponse.buildResponse(response, "success.airquality.historyFetched", messageSource, HttpStatus.OK);
    }

    /**
     * Manually trigger data synchronization (for admin/testing)
     * In production, consider adding @PreAuthorize("hasRole('ADMIN')")
     */
    @PostMapping("/sync/trigger")
    public ResponseEntity<ApiResponse<String>> triggerSync() {
        syncService.triggerManualSync();
        return ApiResponse.buildResponse("Synchronization triggered", "success.airquality.syncTriggered", messageSource, HttpStatus.ACCEPTED);
    }

    /**
     * Find nearest stations to given coordinates
     *
     * @param latitude  Latitude coordinate
     * @param longitude Longitude coordinate
     * @param radius    Search radius in kilometers (default 10, max 100)
     */
    @GetMapping("/stations/nearest")
    public ResponseEntity<ApiResponse<List<StationDistanceResponseDto>>> getNearestStations(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "10") double radius
    ) {
        var stationsWithDistance = airQualityService.findNearestStations(latitude, longitude, radius);
        var response = dtoMapper.toStationDistanceResponseList(stationsWithDistance);
        return ApiResponse.buildResponse(response, "success.airquality.nearestFetched", messageSource, HttpStatus.OK);
    }

    /**
     * Get statistics for a station within time range
     *
     * @param stationId Station ID
     * @param days      Number of days to analyze (default 7, max 90)
     */
    @GetMapping("/stations/{stationId}/statistics")
    public ResponseEntity<ApiResponse<AirQualityStatisticsResponseDto>> getStatistics(
            @PathVariable int stationId,
            @RequestParam(defaultValue = "7") int days
    ) {
        return airQualityService.getStatistics(stationId, days)
                .map(stats -> {
                    var response = dtoMapper.toStatisticsResponse(stats);
                    return ApiResponse.buildResponse(response, "success.airquality.statisticsFetched", messageSource, HttpStatus.OK);
                })
                .orElseThrow(() -> new AirQualityDataNotFoundException(stationId));
    }

    /**
     * Get ranking of stations (best to worst air quality)
     *
     * @param days  Number of days to analyze (default 7, max 90)
     * @param limit Max number of stations to return (default 10, max 50)
     */
    @GetMapping("/stations/ranking")
    public ResponseEntity<ApiResponse<List<StationRankingResponseDto>>> getRanking(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit
    ) {
        var rankings = airQualityService.getRanking(days, limit);
        var response = dtoMapper.toRankingResponseList(rankings);
        return ApiResponse.buildResponse(response, "success.airquality.rankingFetched", messageSource, HttpStatus.OK);
    }
}
