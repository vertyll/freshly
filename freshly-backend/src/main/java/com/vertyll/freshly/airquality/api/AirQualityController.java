package com.vertyll.freshly.airquality.api;

import com.vertyll.freshly.airquality.application.AirQualityService;
import com.vertyll.freshly.airquality.domain.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.Station;
import com.vertyll.freshly.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/air-quality")
@RequiredArgsConstructor
public class AirQualityController {

    private final AirQualityService airQualityService;

    @GetMapping("/stations")
    public ResponseEntity<ApiResponse<List<Station>>> getStations() {
        List<Station> stations = airQualityService.getAllStations();
        return ApiResponse.buildResponse(
                stations,
                "Stations retrieved successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/stations/{stationId}/index")
    public ResponseEntity<ApiResponse<AirQualityIndex>> getIndex(@PathVariable int stationId) {
        return airQualityService.getIndexForStation(stationId)
                .map(index -> ApiResponse.buildResponse(
                        index,
                        "Air quality index retrieved successfully",
                        HttpStatus.OK
                ))
                .orElseGet(() -> ApiResponse.buildResponse(
                        null,
                        "Air quality index not found for station: " + stationId,
                        HttpStatus.NOT_FOUND
                ));
    }

    @GetMapping("/stations/{stationId}/sensors")
    public ResponseEntity<ApiResponse<List<SensorMeasurement>>> getMeasurements(@PathVariable int stationId) {
        List<SensorMeasurement> measurements = airQualityService.getMeasurementsForStation(stationId);
        return ApiResponse.buildResponse(
                measurements,
                "Sensor measurements retrieved successfully",
                HttpStatus.OK
        );
    }
}